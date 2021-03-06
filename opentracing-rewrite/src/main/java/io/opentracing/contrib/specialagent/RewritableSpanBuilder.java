/* Copyright 2020 The OpenTracing Authors
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

package io.opentracing.contrib.specialagent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.tag.Tag;

public class RewritableSpanBuilder extends Rewriter implements Tracer.SpanBuilder {
  final Tracer.SpanBuilder target;

  RewritableSpanBuilder(final String operationName, final Tracer.SpanBuilder target, final RewriteRules rules) {
    super(rules);
    this.target = target;
    onOperationName(operationName);
  }

  @Override
  public Tracer.SpanBuilder asChildOf(final SpanContext parent) {
    target.asChildOf(parent);
    return this;
  }

  @Override
  public Tracer.SpanBuilder asChildOf(final Span parent) {
    target.asChildOf(parent);
    return this;
  }

  @Override
  public Tracer.SpanBuilder addReference(final String referenceType, final SpanContext referencedContext) {
    target.addReference(referenceType, referencedContext);
    return this;
  }

  @Override
  public Tracer.SpanBuilder ignoreActiveSpan() {
    target.ignoreActiveSpan();
    return this;
  }

  @Override
  public Tracer.SpanBuilder withTag(final String key, final String value) {
    onTag(key, value);
    return this;
  }

  @Override
  public Tracer.SpanBuilder withTag(final String key, final boolean value) {
    onTag(key, value);
    return this;
  }

  @Override
  public Tracer.SpanBuilder withTag(final String key, final Number value) {
    onTag(key, value);
    return this;
  }

  @Override
  public <T>Tracer.SpanBuilder withTag(final Tag<T> tag, final T value) {
    onTag(tag.getKey(), value);
    return this;
  }

  @Override
  public Tracer.SpanBuilder withStartTimestamp(final long microseconds) {
    target.withStartTimestamp(microseconds);
    return this;
  }

  @Override
  @Deprecated
  public Span startManual() {
    return target.startManual();
  }

  private RewritableSpan span;

  @Override
  public Span start() {
    final Span span = target.start();
    if (this.span != null && this.span.target == span)
      return this.span;

    if (log != null)
      for (final Map<String,Object> fields : log)
        span.log(fields);

    if (operationName != null)
      span.setOperationName(operationName);

    return this.span = newRewritableSpan(span);
  }

  RewritableSpan newRewritableSpan(final Span span) {
    return new RewritableSpan(span, rules);
  }

  @Override
  @Deprecated
  public Scope startActive(final boolean finishSpanOnClose) {
    return target.startActive(finishSpanOnClose);
  }

  private String operationName;
  private ArrayList<Map<String,Object>> log;

  @Override
  void rewriteTag(final String key, final Object value) {
    if (value == null)
      target.withTag(key, (String)null);
    else if (value instanceof Number)
      target.withTag(key, (Number)value);
    else if (value instanceof Boolean)
      target.withTag(key, (Boolean)value);
    else
      target.withTag(key, value.toString());
  }

  @Override
  void rewriteLog(final long timestampMicroseconds, final String key, final Object value) {
    if (log == null)
      log = new ArrayList<>();

    log.add(Collections.singletonMap(key, value));
  }

  @Override
  void rewriteOperationName(final String name) {
    operationName = name;
  }
}