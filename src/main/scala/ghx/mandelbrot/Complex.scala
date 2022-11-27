package ghx.mandelbrot

class Complex(var r: Double, var i: Double) {
  def +(c: Complex): Complex = {
    //      new Complex(r + c.r, i + c.i)
    r += c.r
    i += c.i
    this
  }
  def *(c: Complex): Complex = {
    val nr = r * c.r - i * c.i
    val ni = r * c.i + c.r * i
    r = nr
    i = ni
    this
  }
  def sq: Complex = {
    //      new Complex(r * r - i * i, 2 * r * i)
    val nr = r * r - i * i
    val ni = 2 * r * i
    r = nr
    i = ni
    this
  }

  def >=(c: Complex): Boolean = module >= c.module

  //    def module = Math.sqrt(r * r + i * i)
  def module = Math.hypot(r, i)

  override def toString = s"Complex($r, $i)"
}
