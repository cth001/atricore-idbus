/**
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
package com.atricore.idbus.console.modeling.main.controller {
import com.atricore.idbus.console.main.controller.AppSectionStartUpCommand;

import com.atricore.idbus.console.main.controller.StartupContext;

import mx.controls.Alert;

import org.puremvc.as3.interfaces.INotification;
import org.springextensions.actionscript.puremvc.interfaces.IIocMediator;

public class ModelerStartUpCommand extends AppSectionStartUpCommand{

    private var _modelerMediator:IIocMediator;

    public function ModelerStartUpCommand() {
    }

    override protected function setupServices(ctx:StartupContext):void {
        super.setupServices(ctx);
        // TODO : Setup services for Modeler
    }

    override protected function setupMediators(ctx:StartupContext):void {
        super.setupMediators(ctx);
        // TODO : Setup mediators for Modeler
        iocFacade.registerMediatorByConfigName(modelerMediator.getConfigName());
    }


    override protected function setupCommands(ctx:StartupContext):void {
        super.setupCommands(ctx);
        // TODO : Setup commands for Modeler
    }


    public function get modelerMediator():IIocMediator {
        return _modelerMediator;
    }

    public function set modelerMediator(value:IIocMediator):void {
        _modelerMediator = value;
    }
}
}
