/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.seiphon.plugin.contributor;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.application.ex.ApplicationUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import org.eclipse.lsp4j.Position;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;
import org.wso2.lsp4intellij.utils.DocumentUtils;
import org.wso2.lsp4intellij.utils.FileUtils;

/**
 * @author Seiphon Wang
 */
public class LiferayCompletionContributor extends CompletionContributor {

	public LiferayCompletionContributor() {
		extend(
			CompletionType.BASIC, usePattern(),
			new CompletionProvider<CompletionParameters>() {

				@Override
				protected void addCompletions(
                        @NotNull CompletionParameters parameters, @NotNull ProcessingContext context,
                        @NotNull CompletionResultSet result) {

					try {
						ApplicationUtil.runWithCheckCanceled(
							() -> {
								Editor editor = parameters.getEditor();

								Position serverPos = DocumentUtils.offsetToLSPPos(editor, parameters.getOffset());

								EditorEventManager manager = EditorEventManagerBase.forEditor(editor);

								if (manager != null) {
									result.addAllElements(manager.completion(serverPos));
								}

								return null;
							},
							ProgressIndicatorProvider.getGlobalProgressIndicator());
					}
					catch (ProcessCanceledException pce) {

						// ProcessCanceledException can be pce.

					}
					catch (Exception e) {
						_log.warn("LSP Completions ended with an error", e);
					}
				}

			});
	}

	@Override
	public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
		CompletionService.getCompletionService().getVariantsFromContributors(parameters, this, completionResult ->
		{
			LookupElement lookupElement = completionResult.getLookupElement();

			result.addElement(lookupElement);
		});
		super.fillCompletionVariants(parameters, result);
	}

	@Override
	public boolean invokeAutoPopup(@NotNull PsiElement position, char typeChar) {
		PsiFile containingFile = position.getContainingFile();

		final VirtualFile file = containingFile.getVirtualFile();

		if (!FileUtils.isFileSupported(file)) {
			return false;
		}

		String uri = FileUtils.VFSToURI(file);

		EditorEventManager manager = EditorEventManagerBase.forUri(uri);

		if (manager == null) {
			return false;
		}

		for (String triggerChar : manager.completionTriggers) {
			if ((triggerChar != null) && (triggerChar.length() == 1) && (triggerChar.charAt(0) == typeChar)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Override this methods and provide a {@link ElementPattern} so that the contributor is
	 * triggered at a location that the pattern is matching. The default pattern which this method
	 * returns is <b>Always True</b>
	 */
	protected ElementPattern<? extends PsiElement> usePattern() {
		return PlatformPatterns.not(PlatformPatterns.alwaysFalse());
	}

	@Override
	public void beforeCompletion(@NotNull CompletionInitializationContext context) {
		if (context.getFile() instanceof PropertiesFile) {
		}
		super.beforeCompletion(context);
	}

	private static final Logger _log = Logger.getInstance(LiferayCompletionContributor.class);

}