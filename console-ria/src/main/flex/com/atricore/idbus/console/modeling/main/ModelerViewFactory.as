/**
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
package com.atricore.idbus.console.modeling.main {
import com.atricore.idbus.console.base.extensions.appsection.AppSectionView;
import com.atricore.idbus.console.base.extensions.appsection.AppSectionViewFactory;

public class ModelerViewFactory extends AppSectionViewFactory {

    public static const VIEW_NAME:String = "ModelerView";

    public function ModelerViewFactory() {
        super(VIEW_NAME);
    }

    override public function createView():AppSectionView {
        return new ModelerView();
    }
}
}