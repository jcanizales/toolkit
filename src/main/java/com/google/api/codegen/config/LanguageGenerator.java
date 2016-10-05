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
package com.google.api.codegen.config;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Generator for language settings section. Currently the only language setting is the package name.
 */
public class LanguageGenerator {

  private static final String DEFAULT_PACKAGE_SEPARATOR = ".";

  private static final String CONFIG_KEY_PACKAGE_NAME = "package_name";

  private static final Map<String, LanguageFormatter> LANGUAGE_FORMATTERS;

  static {
    // Matches API name and version from "google.path1.path2.apiname.version", as $2 and $3.
    String componentsMatcher = "^google(\\.[a-z]+)*\\.([a-z]+)\\.(v[0-9a-z]+)$";

    // TODO(jcanizales): Rewrite this so we can simply specify:
    // java   -> "com.google.cloud.{api}.spi.{version}"
    // python -> "google.cloud.gapic.{api}.{version}"
    // go     -> "cloud.google.com/go/{api}/api{version}"
    // csharp -> "{Path}.{Api}.{Version}"
    // ruby   -> "Google::Cloud::{Api}::{Version}"
    // php    -> "\\Google\\Cloud\\{Api}\\{Version}"
    // nodejs -> "@google-cloud/{api}"
    List<RewriteRule> javaRewriteRules = Arrays.asList(
        new RewriteRule(componentsMatcher, "$2.spi.$3"), // {api}.spi.{version}
        new RewriteRule("^cloud(.+)$", "$1"), // Strip "cloud" prefix from API name.
        new RewriteRule("^(.*)$", "com.google.cloud.$1")); // Prepend com.google.cloud.
    List<RewriteRule> pythonRewriteRules = Arrays.asList(
        new RewriteRule(componentsMatcher, "$2.$3"), // {api}.{version}
        new RewriteRule("^cloud(.+)$", "$1"), // Strip "cloud" prefix from API name.
        new RewriteRule("^(.*)$", "google.cloud.gapic.$1")); // Prepend google.cloud.gapic.
    List<RewriteRule> rubyRewriteRules = Arrays.asList(
        new RewriteRule(componentsMatcher, "$2.$3"), // {api}::{version}
        new RewriteRule("^cloud(.+)$", "$1"), // Strip "cloud" prefix from API name.
        new RewriteRule("^(.*)$", "google.cloud.$1")); // Prepend Google::Cloud::.
    List<RewriteRule> phpRewriteRule = Arrays.asList(
        new RewriteRule(componentsMatcher, "$2.$3"), // {api}\{version}
        new RewriteRule("^cloud(.+)$", "$1"), // Strip "cloud" prefix from API name.
        new RewriteRule("^(.*)$", "google.cloud.$1")); // Prepend Google\Cloud\.
    LANGUAGE_FORMATTERS =
        ImmutableMap.<String, LanguageFormatter>builder()
            .put("java", new SimpleLanguageFormatter(".", javaRewriteRules, false))
            .put("python", new SimpleLanguageFormatter(".", pythonRewriteRules, false))
            .put("go", new GoLanguageFormatter())
            .put("csharp", new SimpleLanguageFormatter(".", null, true))
            .put("ruby", new SimpleLanguageFormatter("::", rubyRewriteRules, true))
            .put("php", new SimpleLanguageFormatter("\\", phpRewriteRule, true))
            .put("nodejs", new NodeJSLanguageFormatter())
            .build();
  }

  public static Map<String, Object> generate(String packageName) {

    Map<String, Object> languages = new LinkedHashMap<>();
    for (String language : LANGUAGE_FORMATTERS.keySet()) {
      LanguageFormatter formatter = LANGUAGE_FORMATTERS.get(language);
      String formattedPackageName = formatter.getFormattedPackageName(packageName);

      Map<String, Object> packageNameMap = new LinkedHashMap<>();
      packageNameMap.put(CONFIG_KEY_PACKAGE_NAME, formattedPackageName);
      languages.put(language, packageNameMap);
    }
    return languages;
  }

  private static String firstCharToUpperCase(String string) {
    return Character.toUpperCase(string.charAt(0)) + string.substring(1);
  }

  /**
   * Returns true if it is a Google Cloud API.
   */
  private static boolean isApiGoogleCloud(List<String> nameComponents) {
    int size = nameComponents.size();
    return size >= 3
        && nameComponents.get(0).equals("google")
        && nameComponents.get(size - 1).startsWith("v");
  }

  private interface LanguageFormatter {
    String getFormattedPackageName(String packageName);
  }

  private static class SimpleLanguageFormatter implements LanguageFormatter {

    private final String separator;
    private final List<RewriteRule> rewriteRules;
    private final boolean capitalize;

    public SimpleLanguageFormatter(
        String separator, List<RewriteRule> rewriteRules, boolean capitalize) {
      this.separator = separator;
      if (rewriteRules != null) {
        this.rewriteRules = rewriteRules;
      } else {
        this.rewriteRules = new ArrayList<>();
      }
      this.capitalize = capitalize;
    }

    public String getFormattedPackageName(String packageName) {
      for (RewriteRule rewriteRule : rewriteRules) {
        packageName = rewriteRule.rewrite(packageName);
      }
      List<String> elements = new LinkedList<>();
      for (String component : Splitter.on(DEFAULT_PACKAGE_SEPARATOR).split(packageName)) {
        if (capitalize) {
          elements.add(firstCharToUpperCase(component));
        } else {
          elements.add(component);
        }
      }
      return Joiner.on(separator).join(elements);
    }
  }

  private static class GoLanguageFormatter implements LanguageFormatter {
    public String getFormattedPackageName(String packageName) {
      List<String> nameComponents = Splitter.on(DEFAULT_PACKAGE_SEPARATOR).splitToList(packageName);

      // If the name follows the pattern google.foo.bar.v1234,
      // we reformat it into cloud.google.com.
      // google.logging.v2 => cloud.google.com/go/logging/apiv2
      // Otherwise, fall back to backup
      if (!isApiGoogleCloud(nameComponents)) {
        nameComponents.add(0, "google.golang.org");
        return Joiner.on("/").join(nameComponents);
      }
      int size = nameComponents.size();
      // If the API name starts with the "cloud" prefix, remove it.
      String name = nameComponents.get(size - 2).replaceFirst("^cloud", "");
      // cloud.google.com/go/{api}/api{version}
      return "cloud.google.com/go/" + name + "/api" + nameComponents.get(size - 1);
    }
  }

  private static class NodeJSLanguageFormatter implements LanguageFormatter {
    public String getFormattedPackageName(String packageName) {
      List<String> nameComponents = Splitter.on(DEFAULT_PACKAGE_SEPARATOR).splitToList(packageName);
      // NodeJS uses "@google-cloud/<APINAME>" for the package name of a Google Cloud API.
      // Otherwise falls back to a pattern of "foo-bar" style with "gax-" prefix.
      if (!isApiGoogleCloud(nameComponents)) {
        return "gax-" + Joiner.on("-").join(nameComponents);
      }
      int size = nameComponents.size();
      // If the API name starts with the "cloud" prefix, remove it.
      String name = nameComponents.get(size - 2).replaceFirst("^cloud", "");
      // @google-cloud/{api}
      return "@google-cloud/" + name;
    }
  }

  private static class RewriteRule {
    private final String pattern;
    private final String replacement;

    public RewriteRule(String pattern, String replacement) {
      this.pattern = pattern;
      this.replacement = replacement;
    }

    public String rewrite(String input) {
      if (pattern == null) {
        return input;
      }
      return input.replaceAll(pattern, replacement);
    }
  }
}
