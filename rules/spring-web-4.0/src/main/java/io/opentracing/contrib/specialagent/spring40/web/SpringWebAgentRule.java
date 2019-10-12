/* Copyright 2019 The OpenTracing Authors
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

package io.opentracing.contrib.specialagent.spring40.web;

import static net.bytebuddy.matcher.ElementMatchers.named;

import io.opentracing.contrib.specialagent.AgentRule;
import java.util.Arrays;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.Transformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.bytecode.assign.Assigner.Typing;
import net.bytebuddy.utility.JavaModule;

public class SpringWebAgentRule extends AgentRule {
  @Override
  public Iterable<? extends AgentBuilder> buildAgent(final AgentBuilder builder) throws Exception {
    return Arrays.asList(builder
        .type(named("org.springframework.web.client.RestTemplate"))
        .transform(new Transformer() {
          @Override
          public Builder<?> transform(final Builder<?> builder, final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module) {
            return builder.visit(Advice.to(RestTemplate.class).on(named("doExecute")));
          }})
        .type(named("org.springframework.web.client.AsyncRestTemplate"))
        .transform(new Transformer() {
          @Override
          public Builder<?> transform(final Builder<?> builder, final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module) {
            return builder.visit(Advice.to(AsyncRestTemplate.class).on(named("doExecute")));
          }}));
  }

  public static class RestTemplate {
    @Advice.OnMethodEnter
    public static void enter(final @Advice.Origin String origin, final @Advice.This Object thiz) {
      if (isEnabled(origin))
        SpringWebAgentIntercept.enter(thiz);
    }
  }

  public static class AsyncRestTemplate {
    @Advice.OnMethodEnter
    public static void enter(final @Advice.Origin String origin, final @Advice.Argument(value = 0, typing = Typing.DYNAMIC) Object url, final @Advice.Argument(value = 1, typing = Typing.DYNAMIC) Object method,
        @Advice.Argument(value = 2, readOnly = false, typing = Typing.DYNAMIC) Object requestCallback) {
      if (isEnabled(origin))
        requestCallback = SpringWebAgentIntercept.asyncStart(url, method, requestCallback);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(final @Advice.Origin String origin, final @Advice.Thrown Throwable thrown, @Advice.Return(readOnly = false, typing = Typing.DYNAMIC) Object response) {
      if (isEnabled(origin))
        response = SpringWebAgentIntercept.asyncEnd(response, thrown);
    }
  }
}