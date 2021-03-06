/* Copyright 2016 Google Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen;

import com.google.api.codegen.nodejs.JSDocCommentFixer;
import com.google.api.codegen.py.PythonSphinxCommentFixer;
import com.google.api.codegen.ruby.RDocCommentFixer;
import com.google.common.truth.Truth;
import java.util.regex.Matcher;
import org.junit.Test;

public class ProtoDocumentLinkTest {

  /* ProtoLink in comments is defined as [display name][fully qualified name] */
  @Test
  public void testMatchProtoLink() {
    Matcher m;
    m = CommentPatterns.PROTO_LINK_PATTERN.matcher("[Shelf][google.example.library.v1.Shelf]");
    Truth.assertThat(m.find()).isTrue();
    // Fully qualified name can be empty
    m = CommentPatterns.PROTO_LINK_PATTERN.matcher("[Shelf][]");
    Truth.assertThat(m.find()).isTrue();
    // Display name may contain special character '$'
    m = CommentPatterns.PROTO_LINK_PATTERN.matcher("[$Shelf][]");
    Truth.assertThat(m.find()).isTrue();
    // Display name may NOT be empty
    m = CommentPatterns.PROTO_LINK_PATTERN.matcher("[][abc]");
    Truth.assertThat(m.find()).isFalse();
    // Fully qualified name may NOT contain special character '$'
    m = CommentPatterns.PROTO_LINK_PATTERN.matcher("[A-Za-z_$][A-Za-z_$0-9]");
    Truth.assertThat(m.find()).isFalse();
    // Fully qualified name may NOT be numbers only
    m = CommentPatterns.PROTO_LINK_PATTERN.matcher("[Shelf][123]");
    Truth.assertThat(m.find()).isFalse();
  }

  @Test
  public void testRDocCommentFixer() {
    Truth.assertThat(RDocCommentFixer.rdocify("[Shelf][google.example.library.v1.Shelf]"))
        .isEqualTo("Shelf");
    Truth.assertThat(RDocCommentFixer.rdocify("[$Shelf][google.example.library.v1.Shelf]"))
        .isEqualTo("$Shelf");

    // Cloud link may contain special character '$'
    Truth.assertThat(RDocCommentFixer.rdocify("[cloud docs!](/library/example/link)"))
        .isEqualTo("{cloud docs!}[https://cloud.google.com/library/example/link]");
    Truth.assertThat(RDocCommentFixer.rdocify("[cloud docs!](/library/example/link$)"))
        .isEqualTo("{cloud docs!}[https://cloud.google.com/library/example/link$]");

    // Absolute link may contain special character '$'
    Truth.assertThat(RDocCommentFixer.rdocify("[not a cloud link](http://www.google.com)"))
        .isEqualTo("{not a cloud link}[http://www.google.com]");
    Truth.assertThat(RDocCommentFixer.rdocify("[not a cloud link](http://www.google.com$)"))
        .isEqualTo("{not a cloud link}[http://www.google.com$]");
  }

  @Test
  public void testPythonSphinxCommentFixer() {
    Truth.assertThat(PythonSphinxCommentFixer.sphinxify("[Shelf][google.example.library.v1.Shelf]"))
        .isEqualTo("``Shelf``");
    Truth.assertThat(
            PythonSphinxCommentFixer.sphinxify("[$Shelf][google.example.library.v1.Shelf]"))
        .isEqualTo("``$Shelf``");

    // Cloud link may contain special character '$'
    Truth.assertThat(PythonSphinxCommentFixer.sphinxify("[cloud docs!](/library/example/link)"))
        .isEqualTo("`cloud docs! <https://cloud.google.com/library/example/link>`_");
    Truth.assertThat(PythonSphinxCommentFixer.sphinxify("[cloud docs!](/library/example/link$)"))
        .isEqualTo("`cloud docs! <https://cloud.google.com/library/example/link$>`_");

    // Absolute link may contain special character '$'
    Truth.assertThat(
            PythonSphinxCommentFixer.sphinxify("[not a cloud link](http://www.google.com)"))
        .isEqualTo("`not a cloud link <http://www.google.com>`_");
    Truth.assertThat(
            PythonSphinxCommentFixer.sphinxify("[not a cloud link](http://www.google.com$)"))
        .isEqualTo("`not a cloud link <http://www.google.com$>`_");
  }

  @Test
  public void testJSDocCommentFixer() {
    Truth.assertThat(JSDocCommentFixer.jsdocify("[Shelf][google.example.library.v1.Shelf]"))
        .isEqualTo("{@link Shelf}");
    Truth.assertThat(JSDocCommentFixer.jsdocify("[$Shelf][google.example.library.v1.Shelf]"))
        .isEqualTo("{@link $Shelf}");

    // Cloud link may contain special character '$'
    Truth.assertThat(JSDocCommentFixer.jsdocify("[cloud docs!](/library/example/link)"))
        .isEqualTo("[cloud docs!](https://cloud.google.com/library/example/link)");
    Truth.assertThat(JSDocCommentFixer.jsdocify("[cloud docs!](/library/example/link$)"))
        .isEqualTo("[cloud docs!](https://cloud.google.com/library/example/link$)");

    // Absolute link may contain special character '$'
    Truth.assertThat(JSDocCommentFixer.jsdocify("[not a cloud link](http://www.google.com)"))
        .isEqualTo("[not a cloud link](http://www.google.com)");
    Truth.assertThat(JSDocCommentFixer.jsdocify("[not a cloud link](http://www.google.com$)"))
        .isEqualTo("[not a cloud link](http://www.google.com$)");
  }
}
