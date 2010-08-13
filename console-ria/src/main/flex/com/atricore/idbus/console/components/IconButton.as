package com.atricore.idbus.console.components {
import spark.components.Button;

//icons
	[Style(name="iconUp",type="Class")]
	[Style(name="iconOver",type="Class")]
	[Style(name="iconDown",type="Class")]
	[Style(name="iconDisabled",type="Class")]
    
	//paddings
	[Style(name="paddingLeft",type="Number")]
	[Style(name="paddingRight",type="Number")]
	[Style(name="paddingTop",type="Number")]
	[Style(name="paddingBottom",type="Number")]
	public class IconButton extends Button
	{
        private var _selected:Boolean;

        private var _isLinkButton:Boolean;

		public function IconButton() {
			super();
		}

        public function get selected():Boolean {
            return _selected;
        }

        public function set selected(value:Boolean):void {
            _selected = value;
        }

        public function get isLinkButton():Boolean {
            return _isLinkButton;
        }

        // toggle hand cursor
        public function set isLinkButton(value:Boolean):void {
            _isLinkButton = value;
            if (_isLinkButton) {
                buttonMode = true;
                useHandCursor = true;
            } else {
                buttonMode = false;
                useHandCursor = false;
            }
        }
    }

}