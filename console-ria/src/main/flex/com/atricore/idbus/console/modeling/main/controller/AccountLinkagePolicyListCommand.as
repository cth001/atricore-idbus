package com.atricore.idbus.console.modeling.main.controller {
import com.atricore.idbus.console.main.ApplicationFacade;
import com.atricore.idbus.console.main.model.ProjectProxy;
import com.atricore.idbus.console.main.service.ServiceRegistry;
import com.atricore.idbus.console.services.spi.request.ListAccountLinkagePoliciesRequest;
import com.atricore.idbus.console.services.spi.response.ListAccountLinkagePoliciesResponse;

import mx.rpc.Fault;
import mx.rpc.IResponder;
import mx.rpc.events.FaultEvent;
import mx.rpc.remoting.mxml.RemoteObject;

import org.puremvc.as3.interfaces.INotification;
import org.springextensions.actionscript.puremvc.patterns.command.IocSimpleCommand;

public class AccountLinkagePolicyListCommand extends IocSimpleCommand implements IResponder {

    public static const SUCCESS:String = "AccountLinkagePolicyListCommand.SUCCESS";
    public static const FAILURE:String = "AccountLinkagePolicyListCommand.FAILURE";

    private var _projectProxy:ProjectProxy;
    private var _registry:ServiceRegistry;


    public function AccountLinkagePolicyListCommand() {
    }

    public function get registry():ServiceRegistry {
        return _registry;
    }

    public function set registry(value:ServiceRegistry):void {
        _registry = value;
    }

    public function get projectProxy():ProjectProxy {
        return _projectProxy;
    }

    public function set projectProxy(value:ProjectProxy):void {
        _projectProxy = value;
    }

    override public function execute(notification:INotification):void {
        var req:ListAccountLinkagePoliciesRequest = new ListAccountLinkagePoliciesRequest();

        var service:RemoteObject = registry.getRemoteObjectService(ApplicationFacade.IDENTITY_APPLIANCE_MANAGEMENT_SERVICE);
        var call:Object = service.listAccountLinkagePolicies(req);
        call.addResponder(this);
    }

    public function result(data:Object):void {
        var resp:ListAccountLinkagePoliciesResponse = data.result as ListAccountLinkagePoliciesResponse;
        projectProxy.accountLinkagePolicies = resp.accountLinkagePolicies;
        sendNotification(SUCCESS);
    }

    public function fault(info:Object):void {
        var fault:Fault = (info as FaultEvent).fault;
        var msg:String = fault.faultString.substring((fault.faultString.indexOf('.') + 1), fault.faultString.length);
        trace(msg);
        sendNotification(FAILURE, msg);
    }
}
}