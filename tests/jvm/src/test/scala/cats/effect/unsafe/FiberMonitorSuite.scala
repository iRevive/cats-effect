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

package cats.effect.unsafe

import cats.effect.{BaseSuite, FiberIO, IO, Outcome}
import cats.effect.std.CountDownLatch
import cats.effect.testkit.TestInstances

import scala.concurrent.duration._

class FiberMonitorSuite extends BaseSuite with TestInstances {

  realWithRuntime("show only active fibers in a live snapshot") { (runtime: IORuntime) =>
    val newline = System.lineSeparator()
    val waitingPattern = raw"cats.effect.IOFiber@[0-9a-f][0-9a-f]+ WAITING((.|$newline)*)"
    val completedPattern = raw"cats.effect.IOFiber@[0-9a-f][0-9a-f]+ COMPLETED"

    for {
      cdl <- CountDownLatch[IO](1)
      fiber <- cdl.await.start // create a 'waiting' fiber
      fiberId <- IO(extractFiberId(fiber))
      _ <- IO.sleep(100.millis)

      snapshot <- IO(makeSnapshot(runtime))
      _ <- IO(assertEquals(snapshot.size, 2)) // root and awaiting fibers
      fiberSnapshot <- IO(snapshot.filter(_.contains(fiberId)))
      _ <- IO(assertEquals(fiberSnapshot.size, 1)) // only awaiting fiber
      _ <- IO(
        assert(fiberSnapshot.exists(_.matches(waitingPattern)), fiberSnapshot.mkString("\n"))
      )

      _ <- cdl.release // allow further execution
      outcome <- fiber.join
      _ <- IO.sleep(100.millis)

      _ <- IO(assertEquals(outcome, Outcome.succeeded[IO, Throwable, Unit](IO.unit)))
      _ <- IO(assert(fiber.toString.matches(completedPattern), fiber.toString))
      _ <- IO(assertEquals(makeSnapshot(runtime).size, 1)) // only root fiber
    } yield ()
  }

  // keep only fibers
  private def makeSnapshot(runtime: IORuntime): List[String] = {
    val builder = List.newBuilder[String]
    runtime.fiberMonitor.liveFiberSnapshot(builder += _)
    builder.result().filter(_.startsWith("cats.effect.IOFiber"))
  }

  private def extractFiberId(fiber: FiberIO[Unit]): String = {
    val pattern = raw"cats.effect.IOFiber@([0-9a-f][0-9a-f]+) .*".r
    pattern.findAllMatchIn(fiber.toString).map(_.group(1)).next()
  }

}
