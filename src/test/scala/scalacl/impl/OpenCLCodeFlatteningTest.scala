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

import scalaxy.streams.WithRuntimeUniverse
import scalaxy.streams.testing.WithTestFresh

import org.junit._
import Assert._
import org.hamcrest.CoreMatchers._

class OpenCLCodeFlatteningTest
    extends OpenCLCodeFlattening
    with WithRuntimeUniverse
    with WithTestFresh {
  import global._

  /*
  def conv(x: Expr[_]) = convert(typeCheck(x))
  def code(statements: Seq[String], values: Seq[String]) =
    FlatCode[String](statements = statements, values = values)
  
  def flattenWithInputSymbols(body: Tree, inputSymbols: Seq[Symbol]): FlatCode[String] = {
    val renamed = renameDefinedSymbolsUniquely(body)
    val tupleAnalyzer = new TupleAnalyzer(renamed)
    val flattener = new TuplesAndBlockFlattener(tupleAnalyzer)
    val Seq(uniqueParam) = inputSymbols
    val flattened = flattener.flattenTuplesAndBlocksWithInputSymbol(renamed, uniqueParam.symbol, uniqueParam.name, currentOwner)(unit)
    
  }
  */
  def unwrap(tree: Tree): Tree = tree match {
    case Block(List(sub), Literal(Constant(()))) => sub
    case _ => tree
  }

  def code(statements: Seq[Expr[_]] = Seq(), values: Seq[Expr[_]] = Seq()) =
    FlatCode[Tree](
      statements = statements.map(x => unwrap(typecheck(x.tree))),
      values = values.map(x => unwrap(typecheck(x.tree)))
    )

  def inputSymbols(xs: Expr[_]*): Seq[(Symbol, Type)] = {
    for (x <- xs.toSeq) yield {
      val i @ Ident(n) = typecheck(x.tree)
      (i.symbol, i.tpe)
    }
  }

  def flat(x: Expr[_], inputSymbols: Seq[(Symbol, Type)] = Seq(), owner: Symbol = NoSymbol): FlatCode[Tree] = {
    flatten(typecheck(x.tree), inputSymbols, owner)
  }

  def assertEquals(a: FlatCode[Tree], b: FlatCode[Tree]) {
    Assert.assertEquals(a.transform(_.toList).toString, b.transform(_.toList).toString)
  }

  @Test
  def simpleTupleValue() {
    val x = 10
    assertEquals(
      code(values = List(
        reify { x },
        reify { x + 1 }
      )),
      flat(
        reify { (x, x + 1) },
        inputSymbols(reify { x })
      )
    )
  }

  @Test
  def simpleTupleReference() {
    val p = (10, 12)
    val Seq(p$1, p$2, pp$1, pp$2) = 1 to 4
    assertEquals(
      code(
        statements = List(
          reify { val pp$1 = p$1 },
          reify { val pp$2 = p$2 }
        ),
        values = List(
          reify { pp$1 },
          reify { pp$2 }
        )
      ),
      flat(
        reify { val pp = p; pp },
        inputSymbols(reify { p })
      )
    )
  }
}
