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
import scala.util.matching.Regex.quoteReplacement

trait CodeConversion
    extends OpenCLConverter
    with StreamTransformers
    with CodeConversionResults
    with UniverseCasts {

  val global: Universe
  import global._
  import definitions._

  def fresh(s: String): String
  // def cleanTypeCheck(tree: Tree): Tree
  def resetLocalAttrs(tree: Tree): Tree
  // def resetAllAttrs(tree: Tree): Tree

  /**
   * Hack to substitute symbols, taking care of stranded symbols.
   * (symbols that sound like one of the matches and are not redefined locally)
   */
  private def substituteSymbols(tree: Tree, replacements: Map[String, (Symbol, Symbol)]): Tree = {
    var currentReplacements = replacements
    var strandedSymbols = Set[Symbol]()
    new Traverser {
      override def traverse(tree: Tree) {
        val sym = tree.symbol
        val oldReplacements = currentReplacements
        var newReplacements = oldReplacements
        if (sym != null && sym != NoSymbol) {
          val name = sym.name.toString
          tree match {
            case d: ValOrDefDef =>
              if (currentReplacements.get(name) != None) {
                newReplacements -= name
              }
            case d: RefTree =>
              for ((expected, replacement) <- currentReplacements.get(name)) {
                if (sym != expected) {
                  // println("Stranded: " + tree + " (sym = " + sym + ", replacement = " + replacement + ")")
                  strandedSymbols += sym
                }
              }
            case _ =>
          }
        }
        currentReplacements = newReplacements
        super.traverse(tree)
        currentReplacements = oldReplacements
      }
    } traverse tree

    val (expectedList, replacementList) = (
      replacements.map(_._2).toList ++
      strandedSymbols.groupBy(_.name.toString).flatMap({
        case (name, stranded) =>
          val replacement = replacements(name)
          stranded.map(s => s -> replacement._2)
      })
    ).unzip

    val res = internal.substituteSymbols(tree, expectedList, replacementList)
    // println(s"""
    // substituteSymbols:
    //   expectedList: $expectedList
    //   replacementList: $replacementList
    //   INIT: $tree
    //   RES: $res
    // """)
    res
  }

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
  /**
   * Transform stream operations, like col.map(...).filter(...).max, to equivalent while loops.
   */
  def transformStreams(tree: Tree, paramDescs: Seq[ParamDesc]): (Tree, Seq[ParamDesc]) = {
    // println(s"""
    //   Generating CL function for:
    //     tree = $tree
    //     paramDescs = $paramDescs
    // """)

    // val toTransform = typeCheck(resetLocalAttrs(tree), WildcardType)
    // println("STREAMS; TO TRANSFORM: " + tree)
    val toTransform = tree
    val transformed = newStreamTransformer(optimizeOnlyIfKnownToBenefit = false).transform(toTransform)
    // println("STREAMS; TRANSFORMED: " + transformed)

    // println(s"""
    //     transformed = $transformed
    // """)
    // val toType = Block(
    //   for (param <- paramDescs.toList) yield {
    //     ValDef(
    //       if (param.output)
    //         Modifiers(Flag.MUTABLE)
    //       else
    //         NoMods,
    //       param.name,
    //       TypeTree(param.tpe),
    //       Literal(Constant(defaultValue(param.tpe)))
    //     )
    //   },
    //   EmptyTree
    //   transformed
    // )
    // println(s"""
    //     toType = $toType
    // """)

    // new Traverser {
    //   override def traverse(tree: Tree) {
    //     val sym = tree.symbol
    //     if (sym != null) {
    //       try {
    //         type XSymbol = {
    //           def typeParams: List[Symbol]
    //         }
    //         sym.asInstanceOf[XSymbol].typeParams
    //       } catch {
    //         case ex: Throwable =>
    //           ex.printStackTrace()
    //           println("ON SYMBOL: " + sym + " IN TREE: " + tree)
    //           throw ex
    //       }
    //     }
    //     super.traverse(tree)
    //   }
    // } traverse toType
    // val Block(valDefs, transformedBody) =
    //   typeCheck(resetLocalAttrs(toType), WildcardType)

    val Block(valDefs, EmptyTree) = typeCheck(
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
    // val valDefs = for ((paramDesc, valDef) <- paramDescs.zip(valDefs0)) yield valDef.substituteSymbols(List(valDef.symbol), List(paramDesc.symbol))

    val toType = Block(valDefs.toList, transformed)

    val Block(_, transformedBody) =
      tryOrTrace("toType = " + toType) {
        typeCheck(resetLocalAttrs(toType), WildcardType)
      }

    // val transformedBody = transformed.substituteSymbols(
    //   paramDescs.map(_.symbol).toList),
    //   valDefs.map(_.symbol))
    // println(s"""
    //     transformedBody = $transformedBody
    // """)
    // val initSyms = paramDescs.map(_.symbol).toSet
    // val initSymNames = initSyms.map(_.name)
    // val newSyms = valDefs.map(_.symbol).toSet
    // val trav = new Traverser {
    //   override def traverse(tree: Tree) {
    //     if (tree.symbol != null) {
    //       if (initSyms.contains(tree.symbol)) {
    //         println("RETAINED OLD SYM: " + tree)
    //       } else if (newSyms.contains(tree.symbol)) {
    //         println("USES NEW SYM: " + tree)
    //       } else if (initSymNames.contains(tree.symbol.name)) {
    //         println("NAME CLASH: " + tree)
    //       }
    //     }
    //     super.traverse(tree)
    //   }
    // }

    // println("TO TRANSFORM:")
    // trav traverse toTransform
    // println("TRANSFORMED:")
    // trav traverse transformed
    // println("TYPED TRANSFORMED BODY:")
    // trav traverse transformedBody

    (
      substituteSymbols(
        transformedBody,
        (for ((paramDesc, valDef) <- paramDescs.zip(valDefs))
          yield paramDesc.name.toString -> ((valDef.symbol, paramDesc.symbol))).toMap),
        paramDescs
    // 
    // paramDescs
    // for ((paramDesc, valDef) <- paramDescs.zip(valDefs)) yield paramDesc.copy(symbol = valDef.symbol)
    )
  }

  def convertCode(tree: Tree, initialParamDescs: Seq[ParamDesc]): CodeConversionResult = {
    val (code, explicitParamDescs) = transformStreams(tree, initialParamDescs)
    // val (code, explicitParamDescs) = (tree, initialParamDescs)

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

    val explicitRangeParams = explicitParamDescs.filter(_.mode == ParamKind.RangeIndex)

    val capturedInputs = capturedParams.filter(p => p.isArray && !p.usage.isOutput)
    val capturedOutputs = capturedParams.filter(p => p.isArray && p.usage.isOutput)
    val capturedConstants =
      capturedParams.filter(!_.isArray) ++
        explicitRangeParams.flatMap(d =>
          Seq(
            //d,
            ParamDesc(
              symbol = d.rangeOffset.get,
              tpe = d.tpe,
              output = false,
              mode = ParamKind.Normal,
              usage = UsageKind.Input),
            ParamDesc(
              symbol = d.rangeStep.get,
              tpe = d.tpe,
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
    // convertCode:
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

    def paramRx(paramDesc: ParamDesc) =
      ("\\b(" + quoteReplacement(paramDesc.symbol.name.toString) + ")\\b").r

    def replacementFunc(r: Regex, rep: String) = {
      // println(s"REPLACEMENT FUNC: $r -> $rep)")
      (s: String) =>
        {
          // println(s"REPLACING $r IN $s")
          r.replaceAllIn(s, rep)
        }
    }
    val replacements: Seq[String => String] = paramDescs.flatMap(paramDesc => {
      val r = paramRx(paramDesc)
      // TODO handle composite types, with replacements of all possible fibers (x._1, x._2._1, x._2._2)
      Option(paramDesc) collect {
        case ParamDesc(_, _, _, ParamKind.ImplicitArrayElement, _, Some(i), None, None) =>
          replacementFunc(r, "$1" + quoteReplacement("[" + globalIDValNames(i) + "]"))
        case ParamDesc(_, _, _, ParamKind.RangeIndex, _, Some(i), Some(from), Some(by)) =>
          replacementFunc(r, quoteReplacement("(" + from.name + " + " + globalIDValNames(i) + " * " + by.name + ")"))
      }
    }) /* ++ explicitRangeParams.map(paramDesc => {
      val r = paramRx(paramDesc)
      val ParamDesc(_, _, _, ParamKind.RangeIndex, _, Some(i), _, _) = paramDesc
      replacementFunc(r, quoteReplacement("(" + globalIDValNames(i) + ")"))
    })*/

    val globalIDStatements = globalIDValNames.toSeq.map {
      case (i, n) =>
        s"size_t $n = get_global_id($i);"
    }

    def replace(s: String) = {
      var r = s
      for (replacement <- replacements)
        r = replacement(r)
      r
    }
    // val result =
    //   (FlatCode[String](statements = globalIDStatements) ++ flat).mapEachValue(s => Seq(
    //     replacements.foldLeft(s)((v, f) => f(v))))

    val result =
      (FlatCode[String](statements = globalIDStatements) ++ flat).map(replace)

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
              internal.typeRef(pre, sym, List(fiberTpe))
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
    var convertedCode =
      result.outerDefinitions.mkString("\n") +
        "kernel void f(" + params.mkString(", ") + ") {\n\t" +
        (result.statements ++ result.values.map(_ + ";")).mkString("\n\t") + "\n" +
        "}"

    if (convertedCode.matches("""(?s).*\bdouble\b.*"""))
      convertedCode = "#pragma OPENCL EXTENSION cl_khr_fp64: enable\n" + convertedCode

    println("convertedCode:\n\t" + convertedCode.replaceAll("\n", "\n\t"))
    CodeConversionResult(convertedCode, capturedInputs, capturedOutputs, capturedConstants)
  }

}
