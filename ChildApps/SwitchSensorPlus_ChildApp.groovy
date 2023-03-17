/**
 *
 * Sensor Groups+_Switch
 *
 * Copyright 2022 Ryan Elliott
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * v1.0		RLE		See parent for changelog
 */
 
definition(
    name: "Sensor Groups+_Switch",
    namespace: "rle.sg+",
    author: "Ryan Elliott",
    description: "Creates a virtual device to track a group of switch devices.",
    category: "Convenience",
	parent: "rle.sg+:Sensor Groups+",
	iconUrl: "",
    iconX2Url: "")

preferences {
    page(name: "mainPage")
}

def mainPage() {
	if(app.getInstallationState() != "COMPLETE") {hide=false} else {hide=true}

	return dynamicPage(name: "mainPage", uninstall:true, install: true) {
		section(getFormat("header","Name"),hideable: true, hidden: hide) {
            label title: getFormat("importantBold","Enter a name for this child app.")+
            getFormat("lessImportant","<br>This will create a virtual switch sensor which reports the on/off status based on the switches you select."), required:true,width:4
			paragraph ""
			input "enforceName", "bool", title: getFormat("importantBold","Sync child device name?")+
            getFormat("lessImportant","<br>Enable: the device and app names will sync.<br>Disabled: the device name can be changed."), defaultValue: true, displayDuringSetup: true, required: false, width: 4
		}

		section(getFormat("header","<b>Device Selection</b>")) {
			paragraph getFormat("importantBold","Please choose which sensors to include in this group.")

			input "switchSensors", "capability.switch" , title: getFormat("lessImportant","Devices to monitor"), multiple:true, required:true,width:4
        }

		section(getFormat("header","<b>Options</b>"),hideable: true, hidden: hide) {            
            input "activeThreshold", "number", title: getFormat("importantBold","How many sensors must be on before the group is on?")+
				getFormat("lessImportant","<br>Leave set to one if any switch on should make the group on."), required:false, defaultValue: 1,width:4
			paragraph ""
			input "infoOutput", "bool", title: "Enable info logging?", defaultValue: true, displayDuringSetup: false, required: false, width: 2
            input "debugOutput", "bool", title: "Enable debug logging?", defaultValue: true, displayDuringSetup: false, required: false, width: 2
        }
	}
}


def installed() {
	initialize()
}

def uninstalled() {
	logDebug "uninstalling app"
	for (device in getChildDevices())
	{
		deleteChildDevice(device.deviceNetworkId)
	}
}

def updated() {	
    logDebug "Updated with settings: ${settings}"
	unschedule()
    unsubscribe()
	initialize()
}

def initialize() {
	subscribe(switchSensors, "switch", switchHandler)
	createOrUpdateChildDevice()
    switchHandler()
    def device = getChildDevice(state.switchDevice)
    device.sendEvent(name: "TotalCount", value: switchSensors.size())
	device.sendEvent(name: "OnThreshold", value: activeThreshold) 
	if (debugOutput) {
		runIn(1800,logsOff)
	}
}

def switchHandler(evt) {
    logInfo "Switch changed, checking status count..."
    getCurrentCount()
    def device = getChildDevice(state.switchDevice)
	if (state.totalOn >= activeThreshold)
	{
		logInfo "On threshold met; setting ${device.displayName} as on"
		logDebug "Current threshold value is ${activeThreshold}"
		device.sendEvent(name: "switch", value: "on", descriptionText: "The on devices are ${state.onList}")
	} else {
		logInfo "On threshold not met; setting ${device.displayName} as off"
		logDebug "Current threshold value is ${activeThreshold}"
		device.sendEvent(name: "switch", value: "off")
	}
}

def createOrUpdateChildDevice() {
	def childDevice = getChildDevice("switchgroup:" + app.getId())
    if (!childDevice || state.switchDevice == null) {
        logDebug "Creating child device"
		state.switchDevice = "switchgroup:" + app.getId()
		addChildDevice("rle.sg+", "Sensor Groups+_OmniSensor", "switchgroup:" + app.getId(), 1234, [label: app.label, isComponent: false])
    }
	if(enforceName) {
		if(childDevice.displayName != app.label) {
			childDevice.label = app.label
		}
	}
}

def getCurrentCount() {
	def device = getChildDevice(state.switchDevice)
	def totalOn = 0
    def totalOff = 0
	def onList = []
	switchSensors.each { it ->
		if (it.currentValue("switch") == "on")
		{
			totalOn++
			onList.add(it.displayName)
		}
		else if (it.currentValue("switch") == "off")
		{
			totalOff++
		}
    }
    state.totalOn = totalOn
	if (onList.size() == 0) {
        onList.add("None")
    }
	onList = onList.sort()
	state.onList = onList
    logDebug "There are ${totalOff} sensors off"
    logDebug "There are ${totalOn} sensors on"
    device.sendEvent(name: "TotalOff", value: totalOff)
    device.sendEvent(name: "TotalOn", value: totalOn)
	device.sendEvent(name: "OnList", value: onList)

	//Create display list
    String displayList = "<ul style='list-style-type: none; margin: 0;padding: 0'>"
	onList.each {it ->
	displayList += "<li>${it}</li>"
	}
	displayList += "</ul>"
	device.sendEvent(name: "displayList", value: displayList)
}

def logInfo(msg) {
    if (settings?.infoOutput) {
		log.info msg
    }
}

def logDebug(msg) {
    if (settings?.debugOutput) {
		log.debug msg
	}
}

def logsOff(){
    log.warn "debug logging disabled..."
    app.updateSetting("debugOutput",[value:"false",type:"bool"])
}

def getFormat(type, myText="") {
	if(type == "header") return "<div style='color:#660000;font-weight: bold'>${myText}</div>"
	if(type == "red") return "<div style='color:#660000'>${myText}</div>"
	if(type == "importantBold") return "<div style='color:#32a4be;font-weight: bold'>${myText}</div>"
	if(type == "important") return "<div style='color:#32a4be'>${myText}</div>"
	if(type == "important2") return "<div style='color:#5a8200'>${myText}</div>"
	if(type == "important2Bold") return "<div style='color:#5a8200;font-weight: bold'>${myText}</div>"
	if(type == "lessImportant") return "<div style='color:green'>${myText}</div>"
	if(type == "rateDisplay") return "<div style='color:green; text-align: center;font-weight: bold'>${myText}</div>"
}