/*
 * Copyright (c) 2011-2015 Pivotal Software Inc., Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package reactor.groovy

import reactor.Environment
import reactor.bus.Event
import reactor.core.dispatch.SynchronousDispatcher
import reactor.groovy.config.GroovyEnvironment
import reactor.groovy.support.ClosureEventConsumer
import spock.lang.Ignore
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import static reactor.bus.selector.Selectors.$

/**
 * @author Stephane Maldini (smaldini)
 */
class GroovyConfigurationSpec extends Specification {

	def "GroovyEnvironment creates dispatcher properly"() {
		when:
			"Building a simple dispatcher"
			Environment env = StaticConfiguration.test().environment()
		then:
			env.defaultDispatcher == env.getDispatcher('test')
	}

	def "GroovyEnvironment creates reactor properly"() {
		when:
			"Building a simple dispatcher"
			GroovyEnvironment groovySystem = StaticConfiguration.test2()
		then:
			groovySystem['test1']
			groovySystem['child_test1']
	}

	def "GroovyEnvironment creates consumers properly"() {
		when:
			"Building a simple dispatcher"
			GroovyEnvironment groovySystem = StaticConfiguration.test3()
			def res = null
			def latch = new CountDownLatch(1)
			groovySystem['test1'].send('test', 'test') {
				res = it
				latch.countDown()
			}
		then:
			latch.await(5, TimeUnit.SECONDS)
			groovySystem['test1'].dispatcher instanceof SynchronousDispatcher
			res
	}

	def "GroovyEnvironment includes another Environment"() {
		when:
			"Building a simple dispatcher"
			GroovyEnvironment groovySystem = StaticConfiguration.test4()
			def res = null
			def latch = new CountDownLatch(1)
			groovySystem['test1'].send('test', 'test') {
				res = it
				latch.countDown()
			}
		then:
			latch.await(5, TimeUnit.SECONDS)
			groovySystem.dispatcher('testDispatcher') instanceof SynchronousDispatcher
			groovySystem['test1'].dispatcher == groovySystem.dispatcher('testDispatcher')
			groovySystem['test2'].dispatcher == groovySystem.dispatcher('testDispatcher')
			res
	}

	def "GroovyEnvironment filters per extension"() {
		when:
			"Building a simple dispatcher"
			GroovyEnvironment groovySystem = StaticConfiguration.test2()

		then:
			groovySystem.reactorBuildersByExtension('a').size() == 2
	}

	@Ignore
	def "GroovyEnvironment intercept with Stream properly"() {
		when:
			"Building a simple dispatcher"
			GroovyEnvironment groovySystem = StaticConfiguration.test5()
			def res = null
			def replyTo = $()
			def consumer = new ClosureEventConsumer({ res = it })
			groovySystem['test1'].on replyTo, consumer
			groovySystem['test1'].send 'test', Event.wrap('test').setReplyTo(replyTo.object)
		then:
			groovySystem['test1'].dispatcher instanceof SynchronousDispatcher
			res == 'intercepted twice'
		when:
			res = null
			replyTo = $()
			groovySystem['test1'].on replyTo, consumer
			groovySystem['test1'].send 'test2', Event.wrap('test').setReplyTo(replyTo.object)
		then:
			res == 'intercepted'
		when:
			res = null
			replyTo = $()
			groovySystem['test2'].on replyTo, consumer
			groovySystem['test2'].send 'test', Event.wrap('test').setReplyTo(replyTo.object)
		then:
			res == null
		when:
			res = null
			replyTo = $()
			groovySystem['test2'].on replyTo, consumer
			groovySystem['test2'].send 'test2', Event.wrap('test').setReplyTo(replyTo.object)
		then:
			res == 'test'
	}

}
