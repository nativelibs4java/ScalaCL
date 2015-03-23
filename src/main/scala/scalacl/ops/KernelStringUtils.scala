package scalacl.ops

trait KernelStringUtils {
  val u: reflect.api.Universe
  import u._
  import definitions._

  def fresh(name: String): String

  def formatConstantTree(value: u.Tree, tpe: u.Type): u.Tree = {

    tpe match {
      case IntTpe | DoubleTpe | BooleanTpe =>
        value
      case ShortTpe =>
        q""" "((short) " + $value + ")" """
      case ByteTpe =>
        q""" "((char) " + $value + ")" """
      case LongTpe =>
        q""" ${value} + "L" """
      case FloatTpe =>
        q""" ${value} + "F" """
      case tpe if tpe != null && tpe =:= typeOf[String] =>
        q""" "\"" + ${value} + "\"" """
      case tpe if tpe != null && tpe <:< typeOf[Array[_]] =>
        val TypeRef(_, _, List(componentTpe)) = tpe
        // println("componentTpe = " + componentTpe)
        val componentFormat = formatConstantTree(Ident(TermName("v")), componentTpe)
        if (componentFormat == null)
          null
        else
          q""" ${value}.map(v => $componentFormat).mkString("{ ", ", ", " }") """

      case OpenCLTupleType(arity, componentTpe) =>
        val compName = TermName(fresh("comp"))
        val componentFormat = formatConstantTree(Ident(compName), componentTpe)
        if (componentFormat == null)
          null
        else {
          val valueName = TermName(fresh("value"))
          q""" {
            val $valueName = $value
            (0 until $arity).map(i => {
              val $compName = $valueName.productElement(i)
              $componentFormat
            }).mkString(${"int" + arity + "("}, ", ", ")")
          } """
        }

      case _ =>
        null
    }
  }

  object OpenCLTupleType {
    val OpenCLTupleSizes = Set(2, 3, 4, 8, 16)
    val OpenCLTupleComponentTypes = Set(
      ByteTpe, ShortTpe, IntTpe, LongTpe, FloatTpe, DoubleTpe)

    def unapply(tpe: Type): Option[(Int, Type)] = Option(tpe) collect {
      case TypeRef(pre, sym, args) if sym.fullName.matches(raw"scala.Tuple\d+") && args.toSet.size == 1 && OpenCLTupleSizes(args.size) && OpenCLTupleComponentTypes(args.head) =>
        (args.size, args.head)
    }
  }

  def formatDefaultConstant(tpe: u.Type): String = {

    tpe match {
      case IntTpe =>
        "0"

      case BooleanTpe =>
        "false"

      case ShortTpe =>
        "((short) 0)"

      case ByteTpe =>
        "((char) 0)"

      case LongTpe =>
        "0L"

      case FloatTpe =>
        "0.0F"

      case DoubleTpe =>
        "0.0"

      case _ if tpe =:= typeOf[String] =>
        "\"\""

      case _ if tpe <:< typeOf[Array[_]] =>
        "{}"

      case OpenCLTupleType(arity, componentTpe) =>
        val constantComponent = formatDefaultConstant(componentTpe)
        if (constantComponent == null)
          null
        else
          (1 to arity).
            map(_ => constantComponent).
            mkString("int" + arity + "(", ", ", ")")

      case _ =>
        null
    }
  }
}
