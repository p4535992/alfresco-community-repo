/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.web.bean.preview;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.TemplateService;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.dialog.NavigationSupport;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.component.UIActionLink;

/**
 * Backing bean for the Preview Space in Template action page
 * 
 * @author Kevin Roast
 */
public class SpacePreviewBean extends BasePreviewBean implements NavigationSupport
{
   private final static String MSG_PREVIEW_OF = "preview_of";
   private final static String MSG_CLOSE = "close";
   /**
    * Returns the Space this bean is currently representing
    * 
    * @return The Space Node
    */
   public Node getNode()
   {
      return this.browseBean.getActionSpace();
   }
   
   /**
    * Returns a model for use by a template on the Document Details page.
    * 
    * @return model containing current document and current space info.
    */
   @SuppressWarnings("unchecked")
   public Map getTemplateModel()
   {
      HashMap model = new HashMap(3, 1.0f);
      
      model.put("space", getNode().getNodeRef());
      model.put(TemplateService.KEY_IMAGE_RESOLVER, imageResolver);
      
      return model;
   }

   protected String finishImpl(FacesContext context, String outcome) throws Exception
   {
      return null;
   }

   public String getCurrentItemId()
   {
      return getId();
   }

   public String getOutcome()
   {
      return "dialog:close:dialog:previewSpace";
   }

   public void nextItem(ActionEvent event)
   {
      boolean foundNextItem = false;
      UIActionLink link = (UIActionLink) event.getComponent();
      Map<String, String> params = link.getParameterMap();
      String id = params.get("id");
      if (id != null && id.length() != 0)
      {
         NodeRef currNodeRef = new NodeRef(Repository.getStoreRef(), id);
         List<Node> nodes = this.browseBean.getParentNodes(currNodeRef);
         if (nodes.size() > 1)
         {
            // perform a linear search - this is slow but stateless
            // otherwise we would have to manage state of last selected node
            // this gets very tricky as this bean is instantiated once and never
            // reset - it does not know when the document has changed etc.
            for (int i = 0; i < nodes.size(); i++)
            {
               if (id.equals(nodes.get(i).getId()) == true)
               {
                  Node next;
                  // found our item - navigate to next
                  if (i != nodes.size() - 1)
                  {
                     next = nodes.get(i + 1);
                  }
                  else
                  {
                     // handle wrapping case
                     next = nodes.get(0);
                  }

                  // prepare for showing details for this node
                  this.browseBean.setupSpaceAction(next.getId(), false);

                  // we found a next item
                  foundNextItem = true;
               }
            }
         }

         // if we did not find a next item make sure the current node is
         // in the dispatch context otherwise the details screen will go back
         // to the default one.
         if (foundNextItem == false)
         {
            Node currNode = new Node(currNodeRef);
            this.navigator.setupDispatchContext(currNode);
         }
      }

   }

   public void previousItem(ActionEvent event)
   {
      boolean foundPreviousItem = false;
      UIActionLink link = (UIActionLink) event.getComponent();
      Map<String, String> params = link.getParameterMap();
      String id = params.get("id");
      if (id != null && id.length() != 0)
      {
         NodeRef currNodeRef = new NodeRef(Repository.getStoreRef(), id);
         List<Node> nodes = this.browseBean.getParentNodes(currNodeRef);
         if (nodes.size() > 1)
         {
            // see above
            for (int i = 0; i < nodes.size(); i++)
            {
               if (id.equals(nodes.get(i).getId()) == true)
               {
                  Node previous;
                  // found our item - navigate to previous
                  if (i != 0)
                  {
                     previous = nodes.get(i - 1);
                  }
                  else
                  {
                     // handle wrapping case
                     previous = nodes.get(nodes.size() - 1);
                  }

                  // show details for this node
                  this.browseBean.setupSpaceAction(previous.getId(), false);

                  // we found a next item
                  foundPreviousItem = true;
               }
            }
         }

         // if we did not find a previous item make sure the current node is
         // in the dispatch context otherwise the details screen will go back
         // to the default one.
         if (foundPreviousItem == false)
         {
            Node currNode = new Node(currNodeRef);
            this.navigator.setupDispatchContext(currNode);
         }
      }

   }

   public String getContainerTitle()
   {
      return Application.getMessage(FacesContext.getCurrentInstance(), MSG_PREVIEW_OF) + " '" + getName() + "'";
   }
   
   public String getCancelButtonLabel()
   {
      return Application.getMessage(FacesContext.getCurrentInstance(), MSG_CLOSE);
   }

}
