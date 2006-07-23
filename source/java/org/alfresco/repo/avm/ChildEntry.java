/*
 * Copyright (C) 2006 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */

package org.alfresco.repo.avm;

/**
 * This interface represents an entry in a directory.
 * @author britt
 */
interface ChildEntry
{
    /**
     * Set the name of the child. 
     * @param name 
     */
    public void setName(String name);
    
    /**
     * Get the name of the child.
     * @return The child's name.
     */
    public String getName();
    
    /**
     * Set the parent in this entry.
     * @param parent
     */
    public void setParent(DirectoryNode parent);
    
    /**
     * Get the parent of this child.
     * @return The parent.
     */
    public DirectoryNode getParent();
    
    /**
     * Set the child in this entry.
     * @param child
     */
    public void setChild(AVMNode child);
    
    /**
     * Get the child in this entry.
     * @return The child.
     */
    public AVMNode getChild();
}
