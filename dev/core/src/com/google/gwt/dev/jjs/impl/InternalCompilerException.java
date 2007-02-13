/*
 * Copyright 2006 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JSourceInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Indicates the compiler encountered an unexpected and unsupported state of
 * operation.
 */
public class InternalCompilerException extends RuntimeException {

  /**
   * Tracks if there's a pending addNode() to avoid recursion sickness.
   */
  private static final ThreadLocal pendingICE = new ThreadLocal();

  /**
   * Information regarding a node that was being processed when an
   * InternalCompilerException was thrown.
   */
  public static final class NodeInfo {

    private final String className;
    private final String description;
    private final JSourceInfo sourceInfo;

    private NodeInfo(String className, String description,
        JSourceInfo sourceInfo) {
      this.className = className;
      this.description = description;
      this.sourceInfo = sourceInfo;
    }

    /**
     * Returns the name of the Java class of the node.
     */
    public String getClassName() {
      return className;
    }

    /**
     * Returns a text description of the node; typically toString().
     */
    public String getDescription() {
      return description;
    }

    /**
     * Returns the node's source info, if available; otherwise <code>null</code>.
     */
    public JSourceInfo getSourceInfo() {
      return sourceInfo;
    }
  }

  private final List nodeTrace = new ArrayList();

  /**
   * Constructs a new exception with the specified node, message, and cause.
   */
  public InternalCompilerException(JNode node, String message, Throwable cause) {
    this(message, cause);
    addNode(node);
  }

  /**
   * Constructs a new exception with the specified message.
   */
  public InternalCompilerException(String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the specified message and cause.
   */
  public InternalCompilerException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Adds a node to the end of the node trace. This is similar to how a stack
   * trace works.
   */
  public void addNode(JNode node) {
    InternalCompilerException other = (InternalCompilerException) pendingICE.get();
    if (other != null) {
      // Avoiding recursion sickness: Yet Another ICE must have occured while
      // generating info for a prior ICE. Just bail!
      return;
    }

    String className = null;
    String description = null;
    JSourceInfo sourceInfo = null;
    try {
      pendingICE.set(this);
      className = node.getClass().getName();
      sourceInfo = node.getSourceInfo();
      description = node.toString();
    } catch (Throwable e) {
      // ignore any exceptions
      if (description == null) {
        description = "<source info not available>";
      }
    } finally {
      pendingICE.set(null);
    }
    addNode(className, description, sourceInfo);
  }

  /**
   * Adds information about a a node to the end of the node trace. This is
   * similar to how a stack trace works.
   */
  public void addNode(String className, String description,
      JSourceInfo sourceInfo) {
    nodeTrace.add(new NodeInfo(className, description, sourceInfo));
  }

  /**
   * Returns a list of nodes that were being processed when this exception was
   * thrown. The list reflects the parent-child relationships of the AST and is
   * is in order from children to parents. The first element of the returned
   * list is the node that was most specifically being visited when the
   * exception was thrown.
   */
  public List getNodeTrace() {
    return nodeTrace;
  }

}
