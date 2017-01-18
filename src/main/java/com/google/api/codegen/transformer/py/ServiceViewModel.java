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
package com.google.api.codegen.transformer.py;

import com.google.api.client.repackaged.com.google.common.base.Preconditions; // ???
import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.gapic.CommonGapicCodePathMapper;
import com.google.api.codegen.py.PythonGapicContext;
import com.google.api.codegen.py.PythonImportHandler;
import com.google.api.codegen.rendering.CommonSnippetSetRunner.TransitionViewModel;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Interface;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

class ServiceViewModel implements ViewModel, TransitionViewModel {

  public static final String SNIPPET_FILE_NAME = "py/main.snip";

  private final Interface service;
  private final ApiConfig apiConfig;
  private final PythonGapicContext context;

  public ServiceViewModel(Interface service, ApiConfig apiConfig) {
    this.service = Preconditions.checkNotNull(service);
    this.apiConfig = Preconditions.checkNotNull(apiConfig);
    this.context = new PythonGapicContext(service.getModel(), apiConfig);
  }

  @Override
  public String resourceRoot() {
    return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
  }

  @Override
  public String templateFileName() {
    return SNIPPET_FILE_NAME;
  }

  @Override
  public String outputPath() {
    String directory =
        CommonGapicCodePathMapper.newBuilder()
            .setShouldAppendPackage(true)
            .build()
            .getOutputPath(service, apiConfig);
    String fileName =
        context.upperCamelToLowerUnderscore(context.getApiWrapperName(service)) + ".py";
    return !Strings.isNullOrEmpty(directory) ? directory + "/" + fileName : fileName;
  }

  /** Used only while transitioning to MVVM. */
  // TODO(jcanizales): Remove references to the model from the View (the snippet), then don't expose them in the ViewModel.
  @Deprecated
  public Interface service() {
    return service;
  }

  @Override
  public Map<String, ?> snippetGlobals() {
    return ImmutableMap.of(
        "context", context, "importHandler", new PythonImportHandler(service, apiConfig));
  }
}