// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.github.pullrequest.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.util.ProgressWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserBase
import com.intellij.openapi.vcs.changes.ui.TreeModelBuilder
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.SideBorder
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.ComponentWithEmptyText
import org.jetbrains.plugins.github.pullrequest.data.GithubPullRequestsChangesLoader
import org.jetbrains.plugins.github.pullrequest.data.SingleWorkerProcessExecutor
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.border.Border
import kotlin.properties.Delegates

class GithubPullRequestChangesComponent(project: Project, loader: GithubPullRequestsChangesLoader)
  : Wrapper(), Disposable, GithubPullRequestsChangesLoader.ChangesLoadingListener, SingleWorkerProcessExecutor.ProcessStateListener {

  private val changesBrowser = PullRequestChangesBrowserWithError(project)
  val toolbarComponent: JComponent = changesBrowser.toolbar.component
  private val changesLoadingPanel = JBLoadingPanel(BorderLayout(), this,
                                                   ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS)

  init {
    loader.addProcessListener(this, this)
    loader.addLoadingListener(this, this)
    changesLoadingPanel.add(changesBrowser, BorderLayout.CENTER)
    setContent(changesLoadingPanel)
    changesBrowser.emptyText.text = DEFAULT_EMPTY_TEXT
  }

  override fun processStarted() {
    changesLoadingPanel.startLoading()
    changesBrowser.emptyText.clear()
    clear()
  }

  override fun processFinished() {
    changesLoadingPanel.stopLoading()
    if (changesBrowser.emptyText.text.isEmpty()) changesBrowser.emptyText.text = DEFAULT_EMPTY_TEXT
  }

  override fun changesLoaded(changes: List<Change>) {
    changesBrowser.changes = changes
    changesBrowser.emptyText.text = "Pull request does not contain any changes"
  }

  override fun errorOccurred(error: Throwable) {
    runInEdt {
      changesBrowser.emptyText
        .appendText("Cannot load changes", SimpleTextAttributes.ERROR_ATTRIBUTES)
        .appendSecondaryText(error.message ?: "Unknown error", SimpleTextAttributes.ERROR_ATTRIBUTES, null)
    }
  }

  private fun clear() {
    changesBrowser.changes = emptyList()
  }

  override fun dispose() {}

  companion object {
    //language=HTML
    private const val DEFAULT_EMPTY_TEXT = "Select pull request to view list of changed files"

    private class PullRequestChangesBrowserWithError(project: Project)
      : ChangesBrowserBase(project, false, false), ComponentWithEmptyText {
      var changes: List<Change> by Delegates.observable(listOf()) { _, _, _ ->
        myViewer.rebuildTree()
      }

      init {
        init()
      }

      override fun buildTreeModel() = TreeModelBuilder.buildFromChanges(myProject, grouping, changes, null)

      override fun onDoubleClick() {
        if (canShowDiff()) super.onDoubleClick()
      }

      override fun getEmptyText() = myViewer.emptyText

      override fun createViewerBorder(): Border = IdeBorderFactory.createBorder(SideBorder.TOP)
    }
  }
}