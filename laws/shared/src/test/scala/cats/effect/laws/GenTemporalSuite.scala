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

package cats
package effect
package laws

import cats.effect.kernel.{Outcome, Temporal}
import cats.effect.kernel.testkit.TimeT
import cats.effect.kernel.testkit.pure._
import cats.syntax.all._

import scala.concurrent.duration._
import munit.FunSuite

import scala.concurrent.TimeoutException

class GenTemporalSuite extends FunSuite {
  outer =>

  type F[A] = PureConc[Throwable, A]

  implicit val F: Temporal[TimeT[F, *]] =
    TimeT.genTemporalForTimeT[F, Throwable]

  val loop: TimeT[F, Unit] = F.sleep(5.millis).foreverM

  test("timeout should return identity when infinite duration") {
    val fa = F.pure(true)
    assertEquals(F.timeout(fa, Duration.Inf), fa)
  }

  test("timeout should return identity when infinite duration") {
    val fa = F.pure(true)
    assertEquals(F.timeout(fa, Duration.Inf), fa)
  }

  test("timeout should succeed on a fast action") {
    val fa: TimeT[F, Boolean] = F.pure(true)
    val op = F.timeout(fa, Duration.Zero)

    assertEquals(
      run(TimeT.run(op)),
      Outcome.Succeeded(Some(true)): Outcome[Option, Throwable, Boolean])
  }

  test("timeout should error out on a slow action") {
    val fa: TimeT[F, Boolean] = F.never *> F.pure(true)
    val op = F.timeout(fa, Duration.Zero)

    run(TimeT.run(op)) match {
      case Outcome.Errored(e) => assert(e.isInstanceOf[TimeoutException])
      case other => fail(s"Expected Outcome.Errored, got $other")
    }
  }

  test("timeout should propagate successful outcome of uncancelable action") {
    val fa = F.uncancelable(_ => F.sleep(50.millis) *> F.pure(true))
    val op = F.timeout(fa, Duration.Zero)

    assertEquals(
      run(TimeT.run(op)),
      Outcome.Succeeded(Some(true)): Outcome[Option, Throwable, Boolean])
  }

  test("timeout should propagate errors from uncancelable action") {
    val fa = F.uncancelable { _ =>
      F.sleep(50.millis) *> F.raiseError(new RuntimeException("fa failed")) *> F.pure(true)
    }
    val op = F.timeout(fa, Duration.Zero)

    run(TimeT.run(op)) match {
      case Outcome.Errored(e: RuntimeException) => assertEquals(e.getMessage, "fa failed")
      case other => fail(s"Expected Outcome.Errored, got $other")
    }
  }

  test("timeoutTo should return identity when infinite duration") {
    val fa: TimeT[F, Boolean] = F.pure(true)
    val fallback: TimeT[F, Boolean] = F.raiseError(new RuntimeException)
    assertEquals(F.timeoutTo(fa, Duration.Inf, fallback), fa)
  }

  test("timeoutTo should return identity when infinite duration") {
    val fa: TimeT[F, Boolean] = F.pure(true)
    val fallback: TimeT[F, Boolean] = F.raiseError(new RuntimeException)
    assertEquals(F.timeoutTo(fa, Duration.Inf, fallback), fa)
  }

  test("timeoutTo should succeed on a fast action") {
    val fa: TimeT[F, Boolean] = F.pure(true)
    val fallback: TimeT[F, Boolean] = F.raiseError(new RuntimeException)
    val op = F.timeoutTo(fa, Duration.Zero, fallback)

    assertEquals(
      run(TimeT.run(op)),
      Outcome.Succeeded(Some(true)): Outcome[Option, Throwable, Boolean])
  }

  test("timeoutTo should error out on a slow action") {
    val fa: TimeT[F, Boolean] = F.never *> F.pure(true)
    val fallback: TimeT[F, Boolean] = F.raiseError(new RuntimeException)
    val op = F.timeoutTo(fa, Duration.Zero, fallback)

    run(TimeT.run(op)) match {
      case Outcome.Errored(e) => assert(e.isInstanceOf[RuntimeException])
      case other => fail(s"Expected OutcomeErrored, got $other")
    }
  }

  test("timeoutTo should propagate successful outcome of uncancelable action") {
    val fa = F.uncancelable(_ => F.sleep(50.millis) *> F.pure(true))
    val fallback: TimeT[F, Boolean] = F.raiseError(new RuntimeException)
    val op = F.timeoutTo(fa, Duration.Zero, fallback)

    assertEquals(
      run(TimeT.run(op)),
      Outcome.Succeeded(Some(true)): Outcome[Option, Throwable, Boolean])
  }

  test("timeoutTo should propagate errors from uncancelable action") {
    val fa = F.uncancelable { _ =>
      F.sleep(50.millis) *> F.raiseError(new RuntimeException("fa failed")) *> F.pure(true)
    }
    val fallback: TimeT[F, Boolean] = F.raiseError(new RuntimeException)
    val op = F.timeoutTo(fa, Duration.Zero, fallback)

    run(TimeT.run(op)) match {
      case Outcome.Errored(e: RuntimeException) => assertEquals(e.getMessage, "fa failed")
      case other => fail(s"Expected Outcome.Errored, got $other")
    }
  }

  test("timeoutAndForget should return identity when infinite duration") {
    val fa: TimeT[F, Boolean] = F.pure(true)
    assertEquals(F.timeoutAndForget(fa, Duration.Inf), fa)
  }

  // TODO enable these tests once Temporal for TimeT is fixed
  /*"temporal" should {
      "cancel a loop" in {
        val op: TimeT[F, Either[Throwable, Unit]] = F.timeout(loop, 5.millis).attempt

        run(TimeT.run(op)) must beLike {
          case Succeeded(Some(Left(e))) => e must haveClass[TimeoutException]
        }
      }.pendingUntilFixed
    }

    "timeoutTo" should {
      "use fallback" in {
        val op: TimeT[F, Boolean] = F.timeoutTo(loop >> F.pure(false), 5.millis, F.pure(true))

        run(TimeT.run(op)) , Succeeded(Some(true))
      }.pendingUntilFixed
    }
  }*/

}
