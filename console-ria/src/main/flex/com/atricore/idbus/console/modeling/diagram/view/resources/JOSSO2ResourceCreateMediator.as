package com.atricore.idbus.console.modeling.diagram.view.resources {
import com.atricore.idbus.console.main.ApplicationFacade;
import com.atricore.idbus.console.main.model.ProjectProxy;
import com.atricore.idbus.console.main.view.form.FormUtility;
import com.atricore.idbus.console.main.view.form.IocFormMediator;
import com.atricore.idbus.console.modeling.palette.PaletteMediator;
import com.atricore.idbus.console.services.dto.JOSSO2Resource;

import flash.events.MouseEvent;

import mx.events.CloseEvent;

import org.puremvc.as3.interfaces.INotification;

public class JOSSO2ResourceCreateMediator extends IocFormMediator {

    private var _projectProxy:ProjectProxy;
    private var _newResource:JOSSO2Resource;

    public function JOSSO2ResourceCreateMediator(name:String = null, viewComp:JOSSO2ResourceCreateForm = null) {
        super(name, viewComp);
    }

    public function get projectProxy():ProjectProxy {
        return _projectProxy;
    }

    public function set projectProxy(value:ProjectProxy):void {
        _projectProxy = value;
    }

    override public function setViewComponent(viewComponent:Object):void {
        if (getViewComponent() != null) {
            view.btnOk.removeEventListener(MouseEvent.CLICK, handleResourceSave);
            view.btnCancel.removeEventListener(MouseEvent.CLICK, handleCancel);
        }

        super.setViewComponent(viewComponent);

        init();
    }

    private function init():void {
        view.btnOk.addEventListener(MouseEvent.CLICK, handleResourceSave);
        view.btnCancel.addEventListener(MouseEvent.CLICK, handleCancel);
        view.focusManager.setFocus(view.resourceName);
    }

    private function resetForm():void {
        view.resourceName.text = "";
        view.resourceDescription.text = "";

        FormUtility.clearValidationErrors(_validators);
    }

    override public function bindModel():void {
        var resource:JOSSO2Resource = new JOSSO2Resource();

        resource.name = view.resourceName.text;
        resource.description = view.resourceDescription.text;

        _newResource = resource;
    }

    private function handleResourceSave(event:MouseEvent):void {
        if (validate(true)) {
            bindModel();
            _projectProxy.currentIdentityAppliance.idApplianceDefinition.serviceResources.addItem(_newResource);
            _projectProxy.currentIdentityApplianceElement = _newResource;
            sendNotification(ApplicationFacade.DIAGRAM_ELEMENT_CREATION_COMPLETE);
            sendNotification(ApplicationFacade.UPDATE_IDENTITY_APPLIANCE);
            sendNotification(ApplicationFacade.IDENTITY_APPLIANCE_CHANGED);
            closeWindow();
        }
        else {
            event.stopImmediatePropagation();
        }
    }

    private function handleCancel(event:MouseEvent):void {
        closeWindow();
    }

    private function closeWindow():void {
        resetForm();
        sendNotification(PaletteMediator.DESELECT_PALETTE_ELEMENT);
        view.parent.dispatchEvent(new CloseEvent(CloseEvent.CLOSE));
    }

    protected function get view():JOSSO2ResourceCreateForm {
        return viewComponent as JOSSO2ResourceCreateForm;
    }

    override public function registerValidators():void {
        _validators.push(view.nameValidator);
    }

    override public function listNotificationInterests():Array {
        return super.listNotificationInterests();
    }

    override public function handleNotification(notification:INotification):void {
        super.handleNotification(notification);
    }
}
}