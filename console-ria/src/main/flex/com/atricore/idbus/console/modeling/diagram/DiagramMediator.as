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

package com.atricore.idbus.console.modeling.diagram
{
import com.atricore.idbus.console.components.CustomEdgeLabelRenderer;
import com.atricore.idbus.console.components.CustomEdgeRenderer;
import com.atricore.idbus.console.components.CustomVisualGraph;
import com.atricore.idbus.console.main.ApplicationFacade;
import com.atricore.idbus.console.main.model.ProjectProxy;
import com.atricore.idbus.console.main.view.util.Constants;
import com.atricore.idbus.console.modeling.browser.model.BrowserModelFactory;
import com.atricore.idbus.console.modeling.browser.model.BrowserNode;
import com.atricore.idbus.console.modeling.diagram.event.VEdgeSelectedEvent;
import com.atricore.idbus.console.modeling.diagram.event.VNodeRemoveEvent;
import com.atricore.idbus.console.modeling.diagram.event.VNodeSelectedEvent;
import com.atricore.idbus.console.modeling.diagram.event.VNodesLinkedEvent;
import com.atricore.idbus.console.modeling.diagram.model.GraphDataManager;
import com.atricore.idbus.console.modeling.diagram.model.request.CreateExecutionEnvironmentElementRequest;
import com.atricore.idbus.console.modeling.diagram.model.request.CreateIdentityProviderElementRequest;
import com.atricore.idbus.console.modeling.diagram.model.request.CreateIdentityVaultElementRequest;
import com.atricore.idbus.console.modeling.diagram.model.request.CreateIdpChannelElementRequest;
import com.atricore.idbus.console.modeling.diagram.model.request.CreateLdapIdentitySourceElementRequest;
import com.atricore.idbus.console.modeling.diagram.model.request.CreateServiceProviderElementRequest;
import com.atricore.idbus.console.modeling.diagram.model.request.CreateSpChannelElementRequest;
import com.atricore.idbus.console.modeling.diagram.model.request.RemoveIdentityApplianceElementRequest;
import com.atricore.idbus.console.modeling.diagram.model.request.RemoveIdentityProviderElementRequest;
import com.atricore.idbus.console.modeling.diagram.model.request.RemoveIdentityVaultElementRequest;
import com.atricore.idbus.console.modeling.diagram.model.request.RemoveIdpChannelElementRequest;
import com.atricore.idbus.console.modeling.diagram.model.request.RemoveServiceProviderElementRequest;
import com.atricore.idbus.console.modeling.diagram.model.request.RemoveSpChannelElementRequest;
import com.atricore.idbus.console.modeling.diagram.renderers.node.NodeDetailedRenderer;
import com.atricore.idbus.console.services.dto.DbIdentitySource;
import com.atricore.idbus.console.services.dto.EmbeddedIdentitySource;
import com.atricore.idbus.console.services.dto.FederatedProvider;
import com.atricore.idbus.console.services.dto.IdentityAppliance;
import com.atricore.idbus.console.services.dto.IdentityApplianceDefinition;
import com.atricore.idbus.console.services.dto.IdentityProviderChannel;
import com.atricore.idbus.console.services.dto.IdentityProvider;
import com.atricore.idbus.console.services.dto.IdentitySource;
import com.atricore.idbus.console.services.dto.LdapIdentitySource;
import com.atricore.idbus.console.services.dto.LocalProvider;
import com.atricore.idbus.console.services.dto.Provider;
import com.atricore.idbus.console.services.dto.ServiceProviderChannel;
import com.atricore.idbus.console.services.dto.ServiceProvider;

import flash.display.DisplayObject;
import flash.events.MouseEvent;

import mx.collections.ArrayCollection;
import mx.controls.Button;
import mx.core.ClassFactory;
import mx.core.Container;
import mx.utils.UIDUtil;

import org.puremvc.as3.interfaces.INotification;
import org.springextensions.actionscript.puremvc.patterns.mediator.IocMediator;
import org.un.cava.birdeye.ravis.enhancedGraphLayout.data.EnhancedGraph;
import org.un.cava.birdeye.ravis.graphLayout.data.IEdge;
import org.un.cava.birdeye.ravis.graphLayout.data.IGraph;
import org.un.cava.birdeye.ravis.graphLayout.data.INode;
import org.un.cava.birdeye.ravis.graphLayout.layout.CircularLayouter;
import org.un.cava.birdeye.ravis.graphLayout.visual.IVisualNode;
import org.un.cava.birdeye.ravis.utils.TypeUtil;
import org.un.cava.birdeye.ravis.utils.events.VGraphEvent;

public class DiagramMediator extends IocMediator {

    public static const BUNDLE:String = "console";

    public static const ORIENTATION_MENU_ITEM_INDEX:int = 3;

    private var _identityApplianceDiagram:CustomVisualGraph;

    private var _identityAppliance:IdentityAppliance;

    private var _applianceId:String;

    private var _autoFitEnabled:Boolean;

    private var _selectedOrientation:uint;

    private var zoomMax:Number = 2;

    private var zoomMin:Number = .25;

    private var _emptyNotationModel:XML;

    private var _currentlySelectedNode:INode;
    private var _currentlySelectedEdge:IEdge;
    private var _projectProxy:ProjectProxy;

    public function get projectProxy():ProjectProxy {
        return _projectProxy;
    }

    public function set projectProxy(value:ProjectProxy):void {
        _projectProxy = value;
    }

    public function DiagramMediator(name:String = null, viewComp:DiagramView = null) {
        super(name, viewComp);

    }


    override public function setViewComponent(viewComponent:Object):void {
        if (getViewComponent() != null) {
            _identityApplianceDiagram.removeEventListener(VNodeSelectedEvent.VNODE_SELECTED, nodeSelectedEventHandler);
            _identityApplianceDiagram.removeEventListener(VNodeRemoveEvent.VNODE_REMOVE, nodeRemoveEventHandler);
            _identityApplianceDiagram.removeEventListener(VNodesLinkedEvent.VNODES_LINKED, nodesLinkedEventHandler);
            _identityApplianceDiagram.removeEventListener(VEdgeSelectedEvent.VEDGE_SELECTED, edgeSelectedEventHandler);
        }

        super.setViewComponent(viewComponent);

        init();
    }

    private function init():void {

        _identityApplianceDiagram = view.identityApplianceDiagram;
        _identityApplianceDiagram.addEventListener(VNodeSelectedEvent.VNODE_SELECTED, nodeSelectedEventHandler);
        _identityApplianceDiagram.addEventListener(VNodeRemoveEvent.VNODE_REMOVE, nodeRemoveEventHandler);
        _identityApplianceDiagram.addEventListener(VNodesLinkedEvent.VNODES_LINKED, nodesLinkedEventHandler);
        _identityApplianceDiagram.addEventListener(VEdgeSelectedEvent.VEDGE_SELECTED, edgeSelectedEventHandler);
        _emptyNotationModel = <Graph/>;

        resetGraph();
        updateGraph();

    }

    override public function listNotificationInterests():Array {
        return [ApplicationFacade.UPDATE_IDENTITY_APPLIANCE,
            ApplicationFacade.DRAG_ELEMENT_TO_DIAGRAM,
            ApplicationFacade.DIAGRAM_ELEMENT_SELECTED,
            ApplicationFacade.DIAGRAM_ELEMENT_UPDATED,
            ApplicationFacade.DIAGRAM_ELEMENT_REMOVE
        ];
    }

    override public function handleNotification(notification:INotification):void {
        switch (notification.getName()) {
            case ApplicationFacade.UPDATE_IDENTITY_APPLIANCE:
                updateIdentityAppliance();
                init();
                break;
            case ApplicationFacade.DRAG_ELEMENT_TO_DIAGRAM:
                var elementType:int = notification.getBody() as int;

                if (_applianceId != null && elementType == DiagramElementTypes.FEDERATED_CONNECTION_ELEMENT_TYPE) {
                    _identityApplianceDiagram.enterConnectionMode();
                    break;
                }

//                if (_currentlySelectedNode != null) {

                    switch (elementType) {
                        case DiagramElementTypes.IDENTITY_PROVIDER_ELEMENT_TYPE:
                            // assert that source end is an Identity Appliance
//                            if (_currentlySelectedNode.data is IdentityAppliance) {
//                                var ownerIdentityAppliance:IdentityAppliance = _currentlySelectedNode.data as IdentityAppliance;
                               var ownerIdentityAppliance:IdentityAppliance = _identityAppliance;

                                var cip:CreateIdentityProviderElementRequest = new CreateIdentityProviderElementRequest(
                                        ownerIdentityAppliance,
//                                        _currentlySelectedNode.stringid
                                        null
                                        );

                                // this notification will be grabbed by the modeler mediator which will open
                                // the corresponding form
                                sendNotification(ApplicationFacade.CREATE_IDENTITY_PROVIDER_ELEMENT, cip);
//                            }


                            break;
                        case DiagramElementTypes.SERVICE_PROVIDER_ELEMENT_TYPE:
                            // assert that source end is an Identity Appliance
//                            if (_currentlySelectedNode.data is IdentityAppliance) {
//                                var ownerIdentityAppliance:IdentityAppliance = _currentlySelectedNode.data as IdentityAppliance;
                                ownerIdentityAppliance = _identityAppliance;

                                var csp:CreateServiceProviderElementRequest = new CreateServiceProviderElementRequest(
                                        ownerIdentityAppliance,
//                                        _currentlySelectedNode.stringid
                                        null
                                        );

                                // this notification will be grabbed by the modeler mediator which will open
                                // the corresponding form
                                sendNotification(ApplicationFacade.CREATE_SERVICE_PROVIDER_ELEMENT, csp);
//                            }


                            break;
                        case DiagramElementTypes.IDP_CHANNEL_ELEMENT_TYPE:
                            // assert that source end is an Identity Appliance
                            if (_currentlySelectedNode.data is ServiceProvider) {
                                var ownerServiceProvider:ServiceProvider = _currentlySelectedNode.data as ServiceProvider;

                                var cidpc:CreateIdpChannelElementRequest = new CreateIdpChannelElementRequest(
                                        ownerServiceProvider,
                                        _currentlySelectedNode.stringid
                                        );
                                _projectProxy.currentIdentityApplianceElementOwner = ownerServiceProvider;
                                // this notification will be grabbed by the modeler mediator which will open
                                // the corresponding form
                                sendNotification(ApplicationFacade.CREATE_IDP_CHANNEL_ELEMENT, cidpc);
                            }


                            break;
                        case DiagramElementTypes.SP_CHANNEL_ELEMENT_TYPE:
                            // assert that source end is an Identity Appliance
                            if (_currentlySelectedNode.data is IdentityProvider) {
                                var ownerIdentityProvider:IdentityProvider = _currentlySelectedNode.data as IdentityProvider;

                                var csdpc:CreateSpChannelElementRequest = new CreateSpChannelElementRequest(
                                        ownerIdentityProvider,
                                        _currentlySelectedNode.stringid
                                        );
                                _projectProxy.currentIdentityApplianceElementOwner = ownerIdentityProvider;
                                // this notification will be grabbed by the modeler mediator which will open
                                // the corresponding form
                                sendNotification(ApplicationFacade.CREATE_SP_CHANNEL_ELEMENT, csdpc);
                            }


                            break;
                        case DiagramElementTypes.DB_IDENTITY_VAULT_ELEMENT_TYPE:
                            // assert that source end is an Identity Appliance
//                            if (_currentlySelectedNode.data is IdentityAppliance) {
//                                var ownerIdentityAppliance:IdentityAppliance = _currentlySelectedNode.data as IdentityAppliance;
                                ownerIdentityAppliance = _identityAppliance;
                                
                                var civ:CreateIdentityVaultElementRequest = new CreateIdentityVaultElementRequest(
                                        ownerIdentityAppliance,
//                                        _currentlySelectedNode.stringid
                                        null
                                        );

                                // this notification will be grabbed by the modeler mediator which will open
                                // the corresponding form
                                sendNotification(ApplicationFacade.CREATE_DB_IDENTITY_VAULT_ELEMENT, civ);
//                            }


                            break;
                        case DiagramElementTypes.LDAP_IDENTITY_SOURCE_ELEMENT_TYPE:
                            if (_currentlySelectedNode.data is IdentityProvider || _currentlySelectedNode.data is ServiceProvider ) {
                                var ownerObj:Object = _currentlySelectedNode.data;

                                var cliv:CreateLdapIdentitySourceElementRequest = new CreateLdapIdentitySourceElementRequest(
                                        ownerObj,
                                        _currentlySelectedNode.stringid
                                        );

                                // this notification will be grabbed by the modeler mediator which will open
                                // the corresponding form
                                sendNotification(ApplicationFacade.CREATE_LDAP_IDENTITY_SOURCE_ELEMENT, cliv);
                            }


                            break;
                        case DiagramElementTypes.JBOSS_EXECUTION_ENVIRONMENT_ELEMENT_TYPE:
                            if (_currentlySelectedNode.data is ServiceProvider ) {
                                var execEnvironmentSp:ServiceProvider = _currentlySelectedNode.data as ServiceProvider;

                                var ceenv:CreateExecutionEnvironmentElementRequest = new CreateExecutionEnvironmentElementRequest(
                                        execEnvironmentSp,
                                        _currentlySelectedNode.stringid
                                        );
                                _projectProxy.currentIdentityApplianceElementOwner = execEnvironmentSp;
                                // this notification will be grabbed by the modeler mediator which will open
                                // the corresponding form
                                sendNotification(ApplicationFacade.CREATE_JBOSS_EXECUTION_ENVIRONMENT_ELEMENT, ceenv);
                            }
                            break;
                        case DiagramElementTypes.WEBLOGIC_EXECUTION_ENVIRONMENT_ELEMENT_TYPE:
                            if (_currentlySelectedNode.data is ServiceProvider ) {
                                var execEnvironmentSp:ServiceProvider = _currentlySelectedNode.data as ServiceProvider;

                                var ceenv:CreateExecutionEnvironmentElementRequest = new CreateExecutionEnvironmentElementRequest(
                                        execEnvironmentSp,
                                        _currentlySelectedNode.stringid
                                        );
                                _projectProxy.currentIdentityApplianceElementOwner = execEnvironmentSp;
                                // this notification will be grabbed by the modeler mediator which will open
                                // the corresponding form
                                sendNotification(ApplicationFacade.CREATE_WEBLOGIC_EXECUTION_ENVIRONMENT_ELEMENT, ceenv);
                            }
                            break;
                    }
//                }
                break;
            case ApplicationFacade.DIAGRAM_ELEMENT_SELECTED:
                toggleNodeOnByData(_identityApplianceDiagram, _projectProxy.currentIdentityApplianceElement);
                break;

            case ApplicationFacade.DIAGRAM_ELEMENT_UPDATED:
                _identityApplianceDiagram.dispatchEvent(new VGraphEvent(VGraphEvent.VGRAPH_CHANGED));
                break;
            case ApplicationFacade.DIAGRAM_ELEMENT_REMOVE:
                if (_currentlySelectedNode != null) {
                    var elementType:int = notification.getBody() as int;

                    switch (elementType) {
                        case DiagramElementTypes.IDENTITY_APPLIANCE_ELEMENT_TYPE:
                            var identityAppliance:IdentityAppliance = _currentlySelectedNode.data as IdentityAppliance;
                            var ria:RemoveIdentityApplianceElementRequest = new RemoveIdentityApplianceElementRequest(identityAppliance);
                            // this notification will be grabbed by the modeler mediator which will invoke
                            // the corresponding command for processing the removal operation.
                            sendNotification(ApplicationFacade.REMOVE_IDENTITY_APPLIANCE_ELEMENT, ria);
                            break;
                        case DiagramElementTypes.IDENTITY_PROVIDER_ELEMENT_TYPE:
                            var identityProvider:IdentityProvider = _currentlySelectedNode.data as IdentityProvider;

                            var rip:RemoveIdentityProviderElementRequest = new RemoveIdentityProviderElementRequest(identityProvider);

                            // this notification will be grabbed by the modeler mediator which will invoke
                            // the corresponding command for processing the removal operation.
                            sendNotification(ApplicationFacade.REMOVE_IDENTITY_PROVIDER_ELEMENT, rip);
                            break;
                        case DiagramElementTypes.SERVICE_PROVIDER_ELEMENT_TYPE:
                            var serviceProvider:ServiceProvider = _currentlySelectedNode.data as ServiceProvider;

                            var rsp:RemoveServiceProviderElementRequest = new RemoveServiceProviderElementRequest(serviceProvider);

                            // this notification will be grabbed by the modeler mediator which will invoke
                            // the corresponding command for processing the removal operation.
                            sendNotification(ApplicationFacade.REMOVE_SERVICE_PROVIDER_ELEMENT, rsp);
                            break;
                        case DiagramElementTypes.IDP_CHANNEL_ELEMENT_TYPE:
                            var idpChannel:IdentityProviderChannel = _currentlySelectedNode.data as IdentityProviderChannel;

                            var ridpc:RemoveIdpChannelElementRequest = new RemoveIdpChannelElementRequest(idpChannel);

                            // this notification will be grabbed by the modeler mediator which will invoke
                            // the corresponding command for processing the removal operation.
                            sendNotification(ApplicationFacade.REMOVE_IDP_CHANNEL_ELEMENT, ridpc);
                            break;
                        case DiagramElementTypes.SP_CHANNEL_ELEMENT_TYPE:
                            var spChannel:ServiceProviderChannel = _currentlySelectedNode.data as ServiceProviderChannel;

                            var rspc:RemoveSpChannelElementRequest = new RemoveSpChannelElementRequest(spChannel);

                            // this notification will be grabbed by the modeler mediator which will invoke
                            // the corresponding command for processing the removal operation.
                            sendNotification(ApplicationFacade.REMOVE_SP_CHANNEL_ELEMENT, rspc);
                            break;
                        case DiagramElementTypes.DB_IDENTITY_VAULT_ELEMENT_TYPE:
                            var identityVault:DbIdentitySource = _currentlySelectedNode.data as DbIdentitySource;

                            var riv:RemoveIdentityVaultElementRequest = new RemoveIdentityVaultElementRequest(identityVault);

                            // this notification will be grabbed by the modeler mediator which will invoke
                            // the corresponding command for processing the removal operation.
                            sendNotification(ApplicationFacade.REMOVE_DB_IDENTITY_VAULT_ELEMENT, riv);
                            break;
                    }
                }

                if (_currentlySelectedEdge != null) {
                    GraphDataManager.removeVEdge(_identityApplianceDiagram, _currentlySelectedEdge.vedge);
                    _currentlySelectedEdge = null;
                    sendNotification(ApplicationFacade.UPDATE_IDENTITY_APPLIANCE);
                }

                break;
        }

    }

    private function updateIdentityAppliance():void {

        _identityAppliance = projectProxy.currentIdentityAppliance;
    }



    private function updateGraph() {

        if (_identityAppliance != null) {
            _applianceId = _identityAppliance.id.toString();
        } else {
            _applianceId = null;
        }

        resetGraph();

        if (_identityAppliance != null) {
            var identityApplianceDefinition:IdentityApplianceDefinition = _identityAppliance.idApplianceDefinition;

//            var rootGraphNode:IVisualNode = GraphDataManager.addVNodeAsChild(_identityApplianceDiagram, UIDUtil.createUID(), _identityAppliance, null, true, Constants.IDENTITY_BUS_DEEP);
//            rootGraphNode.isVisible = false;

            var vaults:ArrayCollection = new ArrayCollection();
            if (identityApplianceDefinition.identitySources != null) {
                for(var k:int=0; k < identityApplianceDefinition.identitySources.length; k++){
                    var identityVaultNode:BrowserNode = BrowserModelFactory.createIdentityVaultNode(identityApplianceDefinition.identitySources[k], true);
                    var identityVaultGraphNode:IVisualNode = GraphDataManager.addVNodeAsChild(_identityApplianceDiagram, UIDUtil.createUID(), identityApplianceDefinition.identitySources[k], null, true, Constants.PROVIDER_DEEP);
                    vaults.addItem(identityVaultGraphNode);
                }
            }

            if (identityApplianceDefinition.providers != null) {
                for (var i:int = 0; i < identityApplianceDefinition.providers.length; i++) {
                    var provider:Provider = identityApplianceDefinition.providers[i];
                    var providerGraphNode:IVisualNode = GraphDataManager.addVNodeAsChild(_identityApplianceDiagram, UIDUtil.createUID(), provider, null, true, Constants.PROVIDER_DEEP);
                    if (provider is LocalProvider) {
//                        var locProv:LocalProvider = provider as LocalProvider;
                        var provider:Provider = identityApplianceDefinition.providers[i];
                        if (provider is FederatedProvider) {
                            var locProv:FederatedProvider = provider as FederatedProvider;
                            if (locProv.federatedConnectionsA != null && locProv.federatedConnectionsA.length != 0) {
                                if(locProv.identityLookup != null && locProv.identityLookup.identitySource != null){
                                    var idSource:IdentitySource = locProv.identityLookup.identitySource;
                                    //TODO add identitySource and connection towards it
                                    var vaultExists:Boolean = false;
                                    for each (var tmpVaultGraphNode:IVisualNode in vaults){
                                        if(tmpVaultGraphNode.data as IdentitySource == idSource){
                                            GraphDataManager.linkVNodes(_identityApplianceDiagram, tmpVaultGraphNode, providerGraphNode);
                                            vaultExists = true;
                                        }
                                    }
                                    if(!vaultExists){
                                        GraphDataManager.addVNodeAsChild(_identityApplianceDiagram, UIDUtil.createUID(), idSource, providerGraphNode, true, Constants.IDENTITY_VAULT_DEEP);
                                        //if vault doesn't exist in the vaults array, add it so other providers can find it
                                        vaults.addItem(idSource);
                                    }

                                }
                                //TODO ADD CONNECTIONS
                            }
    //                        if (locProv.channels != null) {
    //                            for (var j:int = 0; j < locProv.channels.length; j++) {
    //                                var channel = locProv.channels[j];
    //                                var channelGraphNode:IVisualNode = GraphDataManager.addVNodeAsChild(_identityApplianceDiagram, UIDUtil.createUID(), channel, providerGraphNode, true, Constants.CHANNEL_DEEP);
    //                                var identityVault:IdentitySource = null;
    //                                if (channel is IdentityProviderChannel) {
    //                                    identityVault = IdentityProviderChannel(channel).identityVault;
    //                                } else if (channel is ServiceProviderChannel) {
    //                                    identityVault = ServiceProviderChannel(channel).identityVault;
    //                                }
    //                                if (identityVault != null) {
    //                                    var identityVaultNode:BrowserNode = BrowserModelFactory.createIdentityVaultNode(identityVault, true);
    //                                    //link identity vault with the channel containing it
    ////                                    var identityVaultGraphNode:IVisualNode = GraphDataManager.addVNodeAsChild(_identityApplianceDiagram, UIDUtil.createUID(), identityVault, channelGraphNode, true, Constants.IDENTITY_VAULT_CHANNEL_DEEP);
    //                                    vaultExists = false;
    //                                    for each (tmpVaultGraphNode in vaults){
    //                                        if(tmpVaultGraphNode.data as IdentitySource == identityVault){
    //                                            GraphDataManager.linkVNodes(_identityApplianceDiagram, tmpVaultGraphNode, channelGraphNode);
    //                                            vaultExists = true;
    //                                        }
    //                                    }
    //                                    if(!vaultExists){
    //                                        GraphDataManager.addVNodeAsChild(_identityApplianceDiagram, UIDUtil.createUID(), identityVault, channelGraphNode, true, Constants.IDENTITY_VAULT_CHANNEL_DEEP);
    //                                    }
    //                                }
    //                            }
    //                        }
                            if(locProv is ServiceProvider){
                                var sp:ServiceProvider = locProv as ServiceProvider;
                                if(sp.activation != null && sp.activation.executionEnv != null){  //check for execution environment
                                    var execEnvironment:IVisualNode = GraphDataManager.addVNodeAsChild(_identityApplianceDiagram, UIDUtil.createUID(), sp.activation.executionEnv, providerGraphNode, true, Constants.CHANNEL_DEEP);
                                }
                            }
                        }
                    }
                }
            }
        }

    }


    private function resetGraph():void {
        var graph:IGraph = new EnhancedGraph("Graph", true);
		var vo:Object = TypeUtil.deserializeXMLString(_emptyNotationModel);
		EnhancedGraph(graph).initFromVO(vo);
		_identityApplianceDiagram.graph = graph;

        _identityApplianceDiagram.graph.purgeGraph();
        _identityApplianceDiagram.newNodesDefaultVisible = true;

        _autoFitEnabled = true;
//        _selectedOrientation = HierarchicalLayouter.ORIENT_TOP_DOWN;
//        var layouter:HierarchicalLayouter = new HierarchicalLayouter(_identityApplianceDiagram);
//        layouter.autoFitEnabled = _autoFitEnabled;
//        layouter.orientation = _selectedOrientation;
        var layouter:CircularLayouter = new CircularLayouter(_identityApplianceDiagram);
        layouter.autoFitEnabled = _autoFitEnabled;
//        layouter.orientation = _selectedOrientation;

        _identityApplianceDiagram.layouter = layouter;

        var nodeRenderer:ClassFactory = new ClassFactory(NodeDetailedRenderer);
        _identityApplianceDiagram.itemRenderer = nodeRenderer;

        _identityApplianceDiagram.edgeRenderer = new CustomEdgeRenderer(_identityApplianceDiagram.edgeDrawGraphics);
        _identityApplianceDiagram.edgeLabelRenderer = new ClassFactory(CustomEdgeLabelRenderer);

        /* set if edge labels should be displayed */
        _identityApplianceDiagram.displayEdgeLabels = true;
        _identityApplianceDiagram.displayNodeLabels = true;
        
        _identityApplianceDiagram.draw();
        _identityApplianceDiagram.refresh();

        _identityApplianceDiagram.exitConnectionMode();
    }

    private function nodeSelectedEventHandler(event:VNodeSelectedEvent):void
    {
        var node:INode = _identityApplianceDiagram.graph.nodeByStringId(event.vnodeId);

        toggleUnselectedNodesOff(_identityApplianceDiagram, event.target);
        unselectAllEdges();

        if (node != null) {
            _currentlySelectedNode = node;
            _projectProxy.currentIdentityApplianceElement = node.data;
            sendNotification(ApplicationFacade.DIAGRAM_ELEMENT_SELECTED);
        }
    }

    private function nodeRemoveEventHandler(event:VNodeRemoveEvent):void
    {
        var node:INode = _identityApplianceDiagram.graph.nodeByStringId(event.vnodeId);
        var elementType:int = -1;

        if (node != null) {
            _currentlySelectedNode = node;
            _projectProxy.currentIdentityApplianceElement = node.data;
            //need to add elementType in the notification body for delete func. to work properly
            if(node.data is IdentityAppliance){
                elementType = DiagramElementTypes.IDENTITY_APPLIANCE_ELEMENT_TYPE;
            } else
            if(node.data is IdentityProvider){
                elementType = DiagramElementTypes.IDENTITY_PROVIDER_ELEMENT_TYPE;
            } else
            if (node.data is ServiceProvider){
                elementType = DiagramElementTypes.SERVICE_PROVIDER_ELEMENT_TYPE;
            } else
            if(node.data is IdentityProviderChannel){
                elementType = DiagramElementTypes.IDP_CHANNEL_ELEMENT_TYPE;
            } else
            if(node.data is ServiceProviderChannel){
                elementType = DiagramElementTypes.SP_CHANNEL_ELEMENT_TYPE;
            } else
            if(node.data is DbIdentitySource){
                elementType = DiagramElementTypes.DB_IDENTITY_VAULT_ELEMENT_TYPE;
            }
            //TODO - add other element types

            sendNotification(ApplicationFacade.DIAGRAM_ELEMENT_REMOVE, elementType);
        }
    }

    private function nodesLinkedEventHandler(event:VNodesLinkedEvent):void {
        var node1:IVisualNode = event.vnode1;
        var node2:IVisualNode = event.vnode2;

        // TODO: link node1.data and node2.data
        if ((node1.data is IdentityProvider && node2.data is ServiceProvider) ||
                (node1.data is ServiceProvider && node2.data is IdentityProvider)) {
            // connect IDP and SP
        }

        sendNotification(ApplicationFacade.DIAGRAM_ELEMENT_CREATION_COMPLETE);
        sendNotification(ApplicationFacade.IDENTITY_APPLIANCE_CHANGED);
    }

    private function edgeSelectedEventHandler(event:VEdgeSelectedEvent):void {
        var edge:IEdge = event.edge;

        //var edge:IEdge = _identityApplianceDiagram.edgeByStringId(event.vedgeId);

        toggleUnselectedNodesOff(_identityApplianceDiagram, event.target);

        if (edge != null) {
            _currentlySelectedEdge = edge;
            _projectProxy.currentIdentityApplianceElement = edge.data.data;
            sendNotification(ApplicationFacade.DIAGRAM_ELEMENT_SELECTED);
        }
    }

    private function toggleUnselectedNodesOff(visualCompToCheck:Object, selectedItem:Object):void {

        for each(var obj:Object in visualCompToCheck.getChildren()) {
            if (obj is Container || obj is NodeDetailedRenderer) {
                toggleUnselectedNodesOff(obj, selectedItem);
            } else
            if (obj is Button) {
                    var button:Button = Button(obj);

                    if (button.parent != selectedItem) {
                        button.selected = false;
                    }

            }
        }
    }

    private function unselectAllEdges():void {
        _currentlySelectedEdge = null;
        for each (var edge:IEdge in _identityApplianceDiagram.graph.edges) {
            edge.vedge.lineStyle.color = 0xCCCCCC;
        }
        _identityApplianceDiagram.refresh();
    }

    private function toggleNodeOnByData(visualCompToCheck:Object, targetSemanticElement:Object):void {

        for each(var obj:Object in visualCompToCheck.getChildren()) {
            if (obj is Container && !(obj is NodeDetailedRenderer)) {
                toggleNodeOnByData(obj, targetSemanticElement);
            } else
            if (obj is NodeDetailedRenderer) {
                var renderer:NodeDetailedRenderer = NodeDetailedRenderer(obj);

                var diagramSemanticElement:Object = IVisualNode(renderer.data).data

                var elementFigure:Button = lookUpElementFigure(renderer) as Button;

                if (diagramSemanticElement == targetSemanticElement ) {
                    elementFigure.selected = true;
                } else {
                    elementFigure.selected = false;
                }
            }
        }
    }


    private function lookUpElementFigure(visualCompToCheck:Object):DisplayObject {
        var foundDiagramElement:Button;

        for each(var obj:Object in visualCompToCheck.getChildren()) {
            if (obj is Container || obj is NodeDetailedRenderer) {
                lookUpElementFigure(obj);
            } else
            if (obj is Button) {
                    foundDiagramElement = Button(obj);

            }
        }
        return foundDiagramElement;
    }

    private function findNotationElementBySemanticElement(semanticElement:Object):IVisualNode {
        var foundNode:IVisualNode;

        for each(var node:IVisualNode in _identityApplianceDiagram.graph.nodes) {
            if (node.data != null && node.data == semanticElement ) {
                foundNode = node;
                break;
            }
        }

        return foundNode;
    }



    /**
     * Event handler to be triggered in case the
     * layouter in VGraph has changed.
     * Only used to apply the current value to the new layouter.
     * */
    private function layouterChanged(e:VGraphEvent = null):void {
        _identityApplianceDiagram.layouter.autoFitEnabled = _autoFitEnabled;
        _identityApplianceDiagram.layouter.layoutChanged = true;
    }


    private function zoom(zoomValue:Number):void {
        if (zoomValue <= zoomMin) {
            zoomValue = zoomMin;
            return;
        } else {
            _identityApplianceDiagram.scale = zoomValue;
        }

        if (zoomValue >= zoomMax) {
            zoomValue = zoomMax;
            return;
        } else {
            _identityApplianceDiagram.scale = zoomValue;
        }
    }

    private function zoomWheel(event:MouseEvent):void {
        if (event.ctrlKey) {
            if (event.delta < 0) {
                zoom(_identityApplianceDiagram.scale - .10);
            } else if (event.delta > 0) {
                zoom(_identityApplianceDiagram.scale + .10);
            }
            //zoom(_vgraph.scale + (event.delta * 0.10));
        }
    }

    protected function get view():DiagramView
    {
        return viewComponent as DiagramView;
    }


}
}