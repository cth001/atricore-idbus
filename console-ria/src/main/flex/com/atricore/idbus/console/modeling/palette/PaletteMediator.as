/*
 * Atricore Console
 *
 * Copyright 2009-2010, Atricore Inc.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package com.atricore.idbus.console.modeling.palette {
import com.atricore.idbus.console.main.ApplicationFacade;
import com.atricore.idbus.console.modeling.diagram.DiagramElementTypes;
import com.atricore.idbus.console.modeling.palette.event.PaletteEvent;
import com.atricore.idbus.console.modeling.palette.model.PaletteDrawer;
import com.atricore.idbus.console.modeling.palette.model.PaletteEntry;
import com.atricore.idbus.console.modeling.palette.model.PaletteRoot;

import mx.collections.IList;

import org.puremvc.as3.interfaces.INotification;
import org.puremvc.as3.patterns.observer.Notification;
import org.springextensions.actionscript.puremvc.patterns.mediator.IocMediator;

public class PaletteMediator extends IocMediator {
    private var selectedIndex:int;

    public function PaletteMediator(name : String = null, viewComp:PaletteView = null) {
        super(name, viewComp);


    }

    override public function setViewComponent(viewComponent:Object):void {

        if (getViewComponent() != null) {
            view.removeEventListener(PaletteEvent.CLICK, handlePaletteClick);
        }

        super.setViewComponent(viewComponent);

        init();
    }

    private function init():void {

        // bind view to palette model
        var saml2PaletteDrawer:PaletteDrawer = new PaletteDrawer("SAML 2", null, null);
        saml2PaletteDrawer.add(
                    new PaletteEntry("Identity Provider", null, "Identity Provider Entry", DiagramElementTypes.IDENTITY_PROVIDER_ELEMENT_TYPE)

                );
        saml2PaletteDrawer.add(
                    new PaletteEntry("Service Provider", null, "Service Provider Entry", DiagramElementTypes.SERVICE_PROVIDER_ELEMENT_TYPE)

                );
        saml2PaletteDrawer.add(
                    new PaletteEntry("IDP Channel", null, "Identity Provider Channel Entry", DiagramElementTypes.IDP_CHANNEL_ELEMENT_TYPE)

                );
        saml2PaletteDrawer.add(
                    new PaletteEntry("SP Channel", null, "Service Provider Channel Entry", DiagramElementTypes.SP_CHANNEL_ELEMENT_TYPE)

                );
        saml2PaletteDrawer.add(
                    new PaletteEntry("DB Identity Vault", null, "Database Identity Vault Entry", DiagramElementTypes.DB_IDENTITY_VAULT_ELEMENT_TYPE)

                );

        var pr:PaletteRoot  = new PaletteRoot("Identity Appliance Modeler Palette", null, null);
        pr.add(saml2PaletteDrawer);

        view.rptPaletteRoot.dataProvider = IList(pr);
        view.addEventListener(PaletteEvent.CLICK, handlePaletteClick);

    }


    public function handlePaletteClick(event : PaletteEvent) : void {
       var notification:Notification;

       switch(event.action) {
          case PaletteEvent.ACTION_PALETTE_ITEM_CLICKED :
             var selectedPaletteEntry:PaletteEntry = event.data as PaletteEntry;
             sendNotification(ApplicationFacade.DRAG_ELEMENT_TO_DIAGRAM, selectedPaletteEntry.elementType);
             break;
       }

    }

    override public function listNotificationInterests():Array {
        return super.listNotificationInterests();
    }

    override public function handleNotification(notification:INotification):void {
        super.handleNotification(notification);
    }

    public function get view():PaletteView {
        return viewComponent as PaletteView;
    }

}
}