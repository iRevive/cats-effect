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

import cats.arrow.FunctionK

class ResourceJVMSuite extends BaseSuite {

  /**
   * Recursively calls itself until a [[StackOverflowError]] is encountered, at which point, the
   * current depth is returned.
   *
   * @return
   *   the stack depth at which [[StackOverflowError]] occurs
   */
  def verifyThatSoeIsReproducibleWithStackDepth(): Int = {
    var depth = 0

    def triggerStackOverflowError(n: Int): Int = {
      depth = n
      n + triggerStackOverflowError(n + 1)
    }

    try triggerStackOverflowError(0)
    catch {
      case _: StackOverflowError => depth
    }
  }

  ticked("verify use is stack-safe over binds") { implicit ticker =>
    val stackDepth = verifyThatSoeIsReproducibleWithStackDepth()
    val r = (0 to stackDepth)
      .foldLeft(Resource.eval(IO.unit)) {
        case (r, _) =>
          r.flatMap(_ => Resource.eval(IO.unit))
      }
      .use_
    assertEqv(r, IO.unit)
  }

  real("verify use is stack-safe over binds - 2") {
    val stackDepth = verifyThatSoeIsReproducibleWithStackDepth()
    def p(i: Int): Resource[IO, Int] =
      Resource
        .pure {
          if (i < stackDepth) Left(i + 1)
          else Right(i)
        }
        .flatMap {
          case Left(a) => p(a)
          case Right(b) => Resource.pure(b)
        }

    p(0).use(IO.pure).mustEqual(stackDepth)
  }

  ticked("verify mapK is stack-safe over binds") { implicit ticker =>
    val stackDepth = verifyThatSoeIsReproducibleWithStackDepth()
    val r = (0 to stackDepth)
      .foldLeft(Resource.eval(IO.unit)) {
        case (r, _) =>
          r.flatMap(_ => Resource.eval(IO.unit))
      }
      .mapK(FunctionK.id)
      .use_

    assertEqv(r, IO.unit)
  }

  ticked("verify attempt is stack-safe over binds") { implicit ticker =>
    val stackDepth = verifyThatSoeIsReproducibleWithStackDepth()
    val r = (0 to stackDepth)
      .foldLeft(Resource.eval(IO.unit)) {
        case (r, _) =>
          r.flatMap(_ => Resource.eval(IO.unit))
      }
      .attempt

    assertCompleteAs(r.use_, ())
  }

}
