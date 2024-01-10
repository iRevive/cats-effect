/*
 * Copyright 2020-2023 Typelevel
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

import cats.syntax.all._

class CallbackStackSpec extends BaseSpec {

  "CallbackStack" should {
    "correctly report the number removed" in {
      val stack = CallbackStack[Unit](null)
      val handle = stack.push(_ => ())
      stack.clearHandle(handle)
      stack.pack(1) must beEqualTo(1)
    }

    "handle race conditions in pack" in real {
      IO {
        val stack = CallbackStack[Unit](null)
        locally {
          val handle = stack.push(_ => ())
          stack.clearHandle(handle)
        }
        val clear = {
          val handle = stack.push(_ => ())
          IO(stack.clearHandle(handle))
        }
        (stack, clear)
      }.flatMap {
        case (stack, clear) =>
          val pack = IO(stack.pack(1))
          (pack.both(clear *> pack), pack).mapN {
            case ((x, y), z) =>
              if ((x + y + z) != 2)
                println((x, y, z))
              (x + y + z) must beEqualTo(2)
          }
      }.replicateA_(1000)
        .as(ok)
    }
  }

}
