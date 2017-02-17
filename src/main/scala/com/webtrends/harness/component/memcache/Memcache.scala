/*
 * Copyright 2015 Webtrends (http://www.webtrends.com)
 *
 * See the LICENCE.txt file distributed with this work for additional
 * information regarding copyright ownership.
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
package com.webtrends.harness.component.memcache

import com.twitter.finagle.memcached.Client
import com.twitter.util.{Future, Promise}
import com.webtrends.harness.component.cache.CacheConfig
import com.webtrends.harness.component.metrics.TimerStopwatch
import com.webtrends.harness.component.metrics.metrictype.Histogram
import org.jboss.netty.buffer.ChannelBuffer

case class Memcache(client:Client, config:CacheConfig) {

  val insertedDataBytes = Histogram(s"memcache.${config.namespace}.inserted-data-bytes")
  val failedDataBytes = Histogram(s"memcache.${config.namespace}.failed-data-bytes")

  def get(key:String) : Future[Option[ChannelBuffer]] = {
    val timer = new TimerStopwatch(s"memcache.${config.namespace}.get")
    val p = Promise[Option[ChannelBuffer]]
    val ck = getCacheKey(key)
    ck onSuccess { cKey =>
      val f = client.get(cKey)
      f onSuccess { data =>
        if (data.isDefined) timer.success() else timer.failure()
        p.setValue(data)
      } onFailure { ex =>
        timer.failure()
        p.setException(ex)
      }
    }
    p
  }

  def set(key:String, value:ChannelBuffer) : Future[Boolean] = {
    val timer = new TimerStopwatch(s"memcache.${config.namespace}.set")

    // Will need to be updated if we ever use non array backed ChannelBuffers
    val dataSize: Option[Long] = if (value.hasArray) Some(value.array.length) else None

    val p = Promise[Boolean]
    val ck = getCacheKey(key)
    ck onSuccess { cKey =>
      timer.success()
      dataSize.foreach(insertedDataBytes.update)
      client.set(cKey, value)
      p.setValue(true)
    } onFailure { ex =>
      timer.failure()
      dataSize.foreach(failedDataBytes.update)
      p.setException(ex)
    }
    p
  }

  def delete(key:String) : Future[Boolean] = {
    val timer = new TimerStopwatch(s"memcache.${config.namespace}.delete")
    val p = Promise[Boolean]
    val ck = getCacheKey(key)
    ck onSuccess { cKey =>
      val result = client.delete(cKey)
      result onSuccess { data =>
        timer.success()
        p.setValue(data)
      } onFailure { ex =>
        timer.failure()
        p.setException(ex)
      }
    }
    p
  }

  def increment(key:String, delta:Long) : Future[Option[Long]] = {
    val timer = new TimerStopwatch(s"memcache.${config.namespace}.increment")
    val p = Promise[Option[Long]]
    val ck = getCacheKey(key)
    ck onSuccess { cKey =>
      val result = client.incr(cKey, delta)
      result onSuccess { res =>
        timer.success()
        p.setValue(Some(Long.unbox(res)))
      } onFailure { ex =>
        timer.failure()
        p.setException(ex)
      }
    }
    p
  }

  def decrement(key:String, delta:Long) : Future[Option[Long]] = {
    val timer = new TimerStopwatch(s"memcache.${config.namespace}.decrement")
    val p = Promise[Option[Long]]
    val ck = getCacheKey(key)
    ck onSuccess { cKey =>
      val result = client.decr(cKey, delta)
      result onSuccess { res =>
        timer.success()
        p.setValue(Some(Long.unbox(res)))
      } onFailure { ex =>
        timer.failure()
        p.setException(ex)
      }
    }
    p
  }

  def getCacheKey(key:String) : Future[String] = {
    val timer = new TimerStopwatch(s"memcache.${config.namespace}.getCacheKey")
    val p = Promise[String]
    val currentSet = _getCurrentSet
    currentSet onSuccess { cKey =>
      timer.success()
      p.setValue(_getKey(key, cKey))
    } onFailure { ex =>
      timer.failure()
      p.setValue(_getKey(key, -1))
    }
    p
  }

  private def _getKey(key:String, set:Int) : String = {
    val setKeyPrefix = if (set > -1) set.toString else ""
    setKeyPrefix + "." + config.namespace + "." + key
  }

  private def _getCurrentSet : Future[Int] = {
    val p = Promise[Int]
    if (config.setKey.isEmpty) {
      p.setValue(-1)
    } else {
      val f = client.get(config.setKey)
      f onSuccess { reply =>
        reply match {
          case Some(buffer) => p.setValue(buffer.readInt())
          case None => p.setValue(-1)
        }
      } onFailure { reply =>
        p.setValue(-1)
      }
    }
    p
  }

  def checkHealth() : Future[CacheStatus] = {
    val p = Promise[CacheStatus]
    /*val future = client.stats()
    future onSuccess { reply =>
      p.setValue(CacheStatus(true, "Cache looking good %s".format(reply.toString)))
    } onFailure { fail =>
      p.setValue(CacheStatus(false, fail.getMessage))
    }*/
    p.setValue(CacheStatus(true, "Cache Looking good"))
    p
  }

  def close = {
    client.quit
  }
}
