/*
 * Copyright 2020-2024 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cats.effect

import cats.effect.testkit.TestContext
import munit.{FunSuite, Location, ScalaCheckSuite, TestOptions}
import org.scalacheck.Prop

trait BaseSuite extends FunSuite with Runners {

  @deprecated("Please use a type safe alternative, such as 'real' or 'ticked'", "3.6.0")
  override def test(name: String)(body: => Any)(implicit loc: Location): Unit =
    super.test(name)(body)

  @deprecated("Please use a type safe alternative, such as 'real' or 'ticked'", "3.6.0")
  override def test(options: TestOptions)(body: => Any)(implicit loc: Location): Unit =
    super.test(options)(body)

  @annotation.nowarn("cat=deprecation")
  def testUnit(name: String)(body: => Unit)(implicit loc: Location): Unit =
    test(name)(body)

  @annotation.nowarn("cat=deprecation")
  def testUnit(options: TestOptions)(body: => Unit)(implicit loc: Location): Unit =
    test(options)(body)

  override def munitValueTransforms: List[ValueTransform] =
    super.munitValueTransforms ++ List(
      new ValueTransform("IO", { case _: IO[_] => sys.error("Non-evaluated IO") }),
      new ValueTransform("SyncIO", { case _: SyncIO[_] => sys.error("Non-evaluated SyncIO") })
    )

}

trait BaseScalaCheckSuite extends BaseSuite with ScalaCheckSuite {
  @annotation.nowarn("cat=deprecation")
  def tickedProperty(options: TestOptions)(body: Ticker => Prop)(implicit loc: Location): Unit =
    test(options)(body(Ticker(TestContext())))
}
