/*
 * ScalaCL - putting Scala on the GPU with JavaCL / OpenCL
 * http://scalacl.googlecode.com/
 *
 * Copyright (c) 2009-2013, Olivier Chafik (http://ochafik.com/)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Olivier Chafik nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY OLIVIER CHAFIK AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package scalacl.impl
import scalaxy.components.FlatCode
import scalaxy.components.FlatCodes._
import scalaxy.components.StreamTransformers

import scalacl.CLArray
import scalacl.CLFilteredArray

import scala.reflect.api.Universe
import scala.util.matching.Regex

trait CodeConversion
    extends OpenCLConverter
    with StreamTransformers
    with CodeConversionResults
    with UniverseCasts {

  val global: Universe
  import global._
  import definitions._

  def fresh(s: String): String
  def cleanTypeCheck(tree: Tree): Tree
  def resetLocalAttrs(tree: Tree): Tree
  def resetAllAttrs(tree: Tree): Tree
  // def typeCheck(tree: Tree): Tree
  /*
  ParamDesc(i, ParamKindRangeIndex, Some(0))
    -> get_global_id(0)
  ParamDesc(i, ParamKindRangeIndex, Some(0), Some(from), Some(by))
    -> (from + get_global_id(0) * by)
  ParamDesc(x, ParamKindRead)
    -> x
  ParamDesc(x, ParamKindRead, Some(0))
    -> x[get_global_id(0)]
  */
  def transformStreams(tree: Tree, paramDescs: Seq[ParamDesc]): (Tree, Seq[ParamDesc]) = {
    val Block(valDefs, EmptyTree) =
      typeCheck(
        Block(
          for (param <- paramDescs.toList) yield {
            ValDef(
              if (param.output)
                Modifiers(Flag.MUTABLE)
              else
                NoMods,
              param.name,
              TypeTree(param.tpe),
              Literal(Constant(defaultValue(param.tpe)))
            )
          },
          EmptyTree
        ),
        WildcardType
      )

    val typableBlock =
      Block(valDefs, tree.substituteSymbols(paramDescs.map(_.symbol).toList, valDefs.map(_.symbol)))

    // println(s"""
    //   Generating CL function for:
    //     tree = $tree
    //     paramDescs = $paramDescs
    //     typableBlock = $typableBlock
    // """)

    // val toTransform = typeCheck(typableBlock, WildcardType)
    val toTransform = typableBlock
    val transformed = newStreamTransformer(false).transform(toTransform)

    val Block(_, transformedBody) =
      typeCheck(resetLocalAttrs(transformed), WildcardType)

    println(s"""
        transformedBody = $transformedBody
    """)

    (
      transformedBody,
      for ((paramDesc, valDef) <- paramDescs.zip(valDefs)) yield paramDesc.copy(symbol = valDef.symbol)
    )
  }

  def convertCode(tree: Tree, initialParamDescs: Seq[ParamDesc]): CodeConversionResult = {
    val (code, explicitParamDescs) = transformStreams(tree, initialParamDescs)

    val externalSymbols =
      getExternalSymbols(
        code,
        knownSymbols = explicitParamDescs.map(_.symbol).toSet
      )

    val capturedParams: Seq[ParamDesc] = {
      for (sym <- externalSymbols.capturedSymbols) yield {
        val tpe = externalSymbols.symbolTypes(sym)
        val usage = externalSymbols.symbolUsages(sym)
        ParamDesc(
          sym.asInstanceOf[Symbol],
          tpe.asInstanceOf[Type],
          output = false,
          mode = ParamKind.Normal,
          usage = usage)
      }
    }

    val capturedInputs = capturedParams.filter(p => p.isArray && !p.usage.isOutput)
    val capturedOutputs = capturedParams.filter(p => p.isArray && p.usage.isOutput)
    val capturedConstants =
      capturedParams.filter(!_.isArray) ++
        explicitParamDescs.filter(_.mode == ParamKind.RangeIndex).flatMap(d =>
          Seq(
            ParamDesc(
              symbol = d.rangeOffset.get,
              tpe = IntTpe,
              output = false,
              mode = ParamKind.Normal,
              usage = UsageKind.Input),
            ParamDesc(
              symbol = d.rangeStep.get,
              tpe = IntTpe,
              output = false,
              mode = ParamKind.Normal,
              usage = UsageKind.Input)
          )
        )

    val paramDescs =
      explicitParamDescs ++
        capturedInputs ++
        capturedOutputs ++
        capturedConstants
    // val flat = convert(code)

    // println(s"""
    //   explicitParamDescs: $explicitParamDescs
    //   capturedInputs: $capturedInputs
    //   capturedOutputs: $capturedOutputs
    //   capturedConstants: $capturedConstants
    // """)
    val flat = flattenAndConvert(code, paramDescs.map(d => (d.symbol, d.tpe)))

    val globalIDIndexes =
      paramDescs.flatMap(_.implicitIndexDimension).toSet

    val globalIDValNames: Map[Int, String] =
      globalIDIndexes.map(i => i -> fresh("_global_id_" + i + "_")).toMap

    val replacements: Seq[String => String] = paramDescs.map(paramDesc => {
      val r = ("\\b(" + Regex.quoteReplacement(paramDesc.symbol.name.toString) + ")\\b").r
      // TODO handle composite types, with replacements of all possible fibers (x._1, x._2._1, x._2._2)
      paramDesc match {
        case ParamDesc(_, _, _, ParamKind.ImplicitArrayElement, _, Some(i), None, None) =>
          (s: String) =>
            r.replaceAllIn(s, "$1" + Regex.quoteReplacement("[" + globalIDValNames(i) + "]"))
        case ParamDesc(_, _, _, ParamKind.RangeIndex, _, Some(i), Some(from), Some(by)) =>
          (s: String) =>
            r.replaceAllIn(s, Regex.quoteReplacement("(" + from.name + " + " + globalIDValNames(i) + " * " + by.name + ")"))
        case _ =>
          (s: String) => s
      }
    })

    val globalIDStatements = globalIDValNames.toSeq.map {
      case (i, n) =>
        s"size_t $n = get_global_id($i);"
    }

    val result =
      (FlatCode[String](statements = globalIDStatements) ++ flat).mapEachValue(s => Seq(
        replacements.foldLeft(s)((v, f) => f(v))))

    val params: Seq[String] = paramDescs.filter(_.mode != ParamKind.RangeIndex).flatMap(paramDesc => {
      // TODO handle composite types, with fresh names for each fiber (x_1, x_2_1, x_2_2)
      val rawParamTpe = paramDesc.tpe
      val (paramTpe, isCLArray) = rawParamTpe match {
        case typeRef @ TypeRef(_, _, List(tpe)) if typeRef <:< typeOf[scalacl.CLArray[_]] =>
          tpe -> true
        case tpe =>
          tpe -> false
      }
      //println(s"paramTpe = $paramTpe")

      def getDecl(openclType: String, paramName: String) = {
        (if (paramDesc.isArray) "global " else "") +
          (if (paramDesc.usage == UsageKind.Input && paramDesc.isArray) "const " else "") +
          openclType +
          (if (paramDesc.mode == ParamKind.ImplicitArrayElement) " *" else " ") +
          paramName
      }

      val res = if (isTupleType(paramTpe)) {
        val fiberValues = for ((fiberName, fiberTpe) <- fiberVariableNames(paramDesc.symbol.name, paramTpe)) yield {
          val t = convertTpe({
            if (isCLArray) {
              val TypeRef(pre, sym, args) = typeOf[scalacl.CLArray[Int]]
              TypeRef(pre, sym, List(fiberTpe))
            } else {
              fiberTpe
            }
          })
          getDecl(t, fiberName.toString)
        }
        fiberValues
      } else {
        val t = convertTpe(rawParamTpe)
        Seq(getDecl(t, paramDesc.symbol.name.toString))
      }
      //println(s"res = $res")
      res
    })
    // if (params.toString.contains("noarg"))
    //   throw new RuntimeException()
    val convertedCode =
      result.outerDefinitions.mkString("\n") +
        "kernel void f(" + params.mkString(", ") + ") {\n\t" +
        (result.statements ++ result.values.map(_ + ";")).mkString("\n\t") + "\n" +
        "}"
    // println("convertedCode: " + convertedCode)
    CodeConversionResult(convertedCode, capturedInputs, capturedOutputs, capturedConstants)
  }

}
