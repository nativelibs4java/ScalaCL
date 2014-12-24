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
package scalacl
package impl

import scalacl.impl.FlatCodes._
import scalaxy.streams.Streams

import scala.reflect.NameTransformer

trait OpenCLConverter
    extends OpenCLCodeFlattening
    with Streams
    with KernelSymbolsAnalysis {
  val global: reflect.api.Universe
  import global._
  import definitions._

  private[this] final val UNIT: Unit = ()

  def nodeToStringNoComment(tree: Tree): String = tree.toString() // TODO

  class Ids(start: Long = 1) {
    private var nx = start
    def next = this.synchronized {
      val v = nx
      nx += 1
      v
    }
  }

  var openclLabelIds = new Ids

  // var placeHolderRefs: List[String] = Nil

  def valueCode(v: String) = FlatCode[String](Seq(), Seq(), Seq(v))
  def emptyCode = FlatCode[String](Seq(), Seq(), Seq())
  def statementCode(s: String) = FlatCode[String](Seq(), Seq(s), Seq())

  def flattenAndConvert(
    tree: Tree,
    inputSymbols: Seq[(Symbol, Type)] = Seq(),
    owner: Symbol = NoSymbol,
    renameSymbols: Boolean = true): FlatCode[String] = {
    val flattened = flatten(tree, inputSymbols, owner, renameSymbols)
    val converted = flattened.flatMap(convert)

    // println(s"""
    // flattenAndConvert:
    //   tree: $tree
    //   flattened: $flattened
    //   converted: $converted
    // """)
    converted
  }

  def convert(body: Tree): FlatCode[String] = {
    def cast(expr: Tree, clType: String) =
      convert(expr).mapEachValue(v => Seq("((" + clType + ")" + v + ")"))

    try {
      body match {
        case TupleCreation(tupleArgs) => //Apply(TypeApply(Select(TupleObject(), applyName()), tupleTypes), tupleArgs) if isTopLevel =>
          tupleArgs.map(convert).reduceLeft(_ ++ _)

        case Literal(Constant(value)) =>
          if (value == UNIT)
            emptyCode
          else if (value.isInstanceOf[Float])
            valueCode(s"${value}F")
          else if (value.isInstanceOf[Long])
            valueCode(s"${value}L")
          else
            valueCode(s"$value")

        case Ident(name) =>
          valueCode(name.toString)

        case If(condition, thenDo, otherwise) =>
          // val (a, b) = if ({ val d = 0 ; d != 0 }) (1, d) else (2, 0)
          // ->
          // val d = 0
          // val condition = d != 0
          // val a = if (condition) 1 else 2
          // val b = if (condition) d else 0
          val FlatCode(dc, sc, Seq(vc)) = convert(condition)
          val fct @ FlatCode(Seq(), st, vt) = convert(thenDo)
          val fco @ FlatCode(Seq(), so, vo) = convert(otherwise)

          def newIf(t: String, o: String, isValue: Boolean) =
            if (isValue)
              "((" + vc + ") ? (" + t + ") : (" + o + "))"
            else
              "if (" + vc + ") {\n" + t + "\n} else {\n" + o + "\n}\n"

          val (rs, rv) = (st, so) match {
            case (Seq(), Seq()) if vt.nonEmpty && vo.nonEmpty =>
              (
                Seq(),
                vt.zip(vo).map { case (t, o) => newIf(t, o, isValue = true) } // pure (cond ? then : otherwise) form, possibly with tuple values
              )
            case _ =>
              (
                Seq(newIf((st ++ vt).mkString("\n"), (so ++ vo).mkString("\n"), isValue = false)),
                Seq()
              )
          }
          FlatCode[String](
            dc,
            sc ++ rs,
            rv
          )

        case Apply(Select(target, N("apply")), List(singleArg)) =>
          merge(Seq(target, singleArg).map(convert): _*) { case Seq(t, a) => Seq(t + "[" + a + "]") }

        case Apply(Select(target, N("update")), List(index, value)) =>
          val convs = Seq(target, index, value).map(convert)
          merge(convs: _*) { case Seq(t, i, v) => Seq(t + "[" + i + "] = " + v + ";") }

        case Assign(lhs, rhs) =>
          merge(Seq(lhs, rhs).map(convert): _*) { case Seq(l, r) => Seq(l + " = " + r + ";") }

        case Typed(expr, tpt) =>
          val t = convertTpe(tpt.tpe)
          convert(expr).mapValues(_.map(v => "((" + t + ")" + v + ")"))

        case DefDef(mods, name, tparams, vparamss, tpt, body) =>
          val b = new StringBuilder
          b ++= convertTpe(body.tpe) + " " + name + "("
          var first = true
          for (param <- vparamss.flatten) {
            if (first)
              first = false
            else
              b ++= ", "
            b ++= constPref(param.mods) + convertTpe(param.tpt.tpe) + " " + param.name
          }
          b ++= ") {\n"
          val convBody = convert(body)
          convBody.statements.foreach(b ++= _)
          if (convBody.values.nonEmpty) {
            val Seq(ret) = convBody.values
            b ++= "return " + ret + ";"
          }
          b ++= "\n}"
          FlatCode[String](
            convBody.outerDefinitions :+ b.toString,
            Seq(),
            Seq()
          )

        case vd @ ValDef(paramMods, paramName, tpt: TypeTree, rhs) =>
          val convValue = convert(rhs)
          // println("VD: " + vd)
          FlatCode[String](
            convValue.outerDefinitions,
            convValue.statements ++
              Seq(
                constPref(paramMods) + convertTpt(tpt) + " " + paramName + (
                  if (rhs != EmptyTree) {
                    val Seq(value) = convValue.values
                    " = " + value
                  } else
                    ""
                ) + ";"
              ),
            Seq()
          )
        //case Typed(expr, tpe) =>
        //  out(expr)

        case Match(ma @ Ident(matchName), List(CaseDef(pat, guard, body))) =>
          //for ()
          //x0$1 match {
          //  case (_1: Long,_2: Float)(Long, Float)((i @ _), (c @ _)) => i.+(c)
          //}
          //Match(Ident("x0$1"), List(CaseDef(Apply(TypeTree(), List(Bind(i, Ident("_")), Bind(c, Ident("_"))), EmptyTree Apply(Select(Ident("i"), "$plus"), List(Ident("c")
          convert(body)

        case NumberConversion(expr, typeName) =>
          cast(expr, typeName)

        // TODO
        case ScalaMathFunction(functionType, funName, args) =>
          convertMathFunction(functionType, funName, args)

        case Apply(s @ Select(left, name), args) =>
          val List(right) = args
          NameTransformer.decode(name.toString) match {
            case op @ ("+" | "-" | "*" | "/" | "%" | "^" | "^^" | "&" | "&&" | "|" | "||" | "<<" | ">>" | "==" | "<" | ">" | "<=" | ">=" | "!=") =>
              merge(Seq(left, right).map(convert): _*) {
                case Seq(l, r) => Seq("(" + l + " " + op + " " + r + ")")
                //case e =>
                //  throw new RuntimeException("ugh : " + e + ", op = " + op + ", body = " + body + ", left = " + left + ", right = " + right)
              }

            //merge(Seq(right).map(convert):_*) { case Seq(v) => Seq(n + "(" + v + ")") }
            case n =>
              throw new RuntimeException(
                s"[ScalaCL] Unhandled method name in Scala -> OpenCL conversion : $name\n" +
                  s"\tleft = $left\n" +
                  s"\tleft.sym = ${left.symbol}\n" +
                  s"\targs = $args\n" +
                  s"\tbody = $body\n" +
                  s"\ttree: ${body.getClass.getName}")
              valueCode("/* Error: failed to convert " + body + " */")
          }

        case s @ Select(expr, fun) =>
          convert(expr).mapEachValue(v => {
            val fn = fun.toString
            if (fn.matches("_\\d+")) {
              Seq(v + "." + fn)
            } else {
              throw new RuntimeException("Unknown function " + s)
              Seq("/* Error: failed to convert " + body + " */")
            }
          })

        case WhileLoop(condition, content) =>
          val FlatCode(dcont, scont, vcont) = content.map(convert).reduceLeft(_ >> _)
          val FlatCode(dcond, scond, Seq(vcond)) = convert(condition)
          FlatCode[String](
            dcond ++ dcont,
            scond ++
              Seq(
                "while (" + vcond + ") {\n" +
                  (scont ++ vcont).mkString("\n") + "\n" +
                  "}"
              ),
            Seq()
          )

        case Apply(target, args) =>
          merge((target :: args).map(convert): _*)(seq => {
            val t :: a = seq.toList
            Seq(t + "(" + a.mkString(", ") + ")")
          })

        case Block(statements, Literal(Constant(empty))) =>
          // assert(value == Literal(Constant(UNIT)),
          assert(empty == UNIT,
            s"Valued blocks should have been flattened in a previous phase!\n$empty : ${empty.getClass}")
          statements.map(convert).map(_.noValues).reduceLeft(_ >> _)

        case _ =>
          //println(nodeToStringNoComment(body))
          throw new RuntimeException("Failed to convert " + body.getClass.getName + ": \n" + body + " : \n" + nodeToStringNoComment(body))
          valueCode("/* Error: failed to convert " + body + " */")
      }
    } catch {
      case ex: Throwable =>
        ex.printStackTrace(System.out)
        throw ex
    }
  }
  def convertMathFunction(functionType: Type, funName: Name, args: List[Tree]) = {
    var outers = Seq[String]() //"#include <math.h>")
    val hasDoubleParam = args.exists(_.tpe == DoubleClass.asType.toType)
    if (hasDoubleParam)
      outers ++= Seq("#pragma OPENCL EXTENSION cl_khr_fp64: enable")

    val normalizedArgs = args.map {
      case Select(a, N("toDouble")) => a
      case arg => arg
    }
    val convArgs = normalizedArgs.map(convert)

    assert(convArgs.forall(_.statements.isEmpty), convArgs)
    FlatCode[String](
      convArgs.flatMap(_.outerDefinitions) ++ outers,
      convArgs.flatMap(_.statements),
      Seq(
        funName + "(" +
          convArgs.zip(normalizedArgs).map({
            case (convArg, normalizedArg) =>
              assert(convArg.statements.isEmpty, convArg)
              val Seq(value) = convArg.values
              //"(" + convertTpe(normalizedArg.tpe) + ")" + value
              functionType match {
                case _ //MethodType(List(param), resultType) 
                if normalizedArg.tpe != DoubleClass.asType =>
                  "(float)" + value
                case _ =>
                  "(" + convertTpe(normalizedArg.tpe) + ")" + value
              }
          }).mkString(", ") +
          ")"
      )
    )
  }
  def constPref(mods: Modifiers) =
    if (mods.hasFlag(Flag.MUTABLE)) "" else "const "

  def convertTpt(tpt: TypeTree): String = convertTpe(tpt.tpe)
  def convertTpe(tpe: Type): String = {
    if (tpe == null) {
      sys.error("Null type cannot be converted to OpenCL !")
    } else {
      val t = tpe.dealias
      if (t == NoType || t <:< typeOf[Unit]) "void"
      else if (t <:< IntTpe) "int"
      else if (t <:< LongTpe) "long"
      else if (t <:< ShortTpe || t <:< CharTpe) "short"
      else if (t <:< BooleanTpe || t <:< ByteTpe) "char"
      else if (t <:< DoubleTpe) "double"
      else if (t <:< FloatTpe) "float"
      else if (t <:< typeOf[org.bridj.SizeT]) "size_t"
      else if (t <:< typeOf[CLArray[_]]) {
        val List(target) = t.asInstanceOf[TypeRef].args
        convertTpe(target) + "*"
      } else {
        sys.error("Cannot convert unknown type " + tpe + " (" + t + "): " + tpe.getClass.getName + " to OpenCL")
      }
    }
  }
}
