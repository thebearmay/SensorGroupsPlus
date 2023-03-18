/**
 *
 * Sensor Groups+_Smoke
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
        name: "Sensor Groups+_Smoke",
        namespace: "rle.sg+",
        author: "Ryan Elliott",
        description: "Creates a virtual device to track a group of smoke sensors.",
        category: "Convenience",
        parent: "rle.sg+:Sensor Groups+",
        iconUrl: "",
        iconX2Url: "")

preferences {
    page(name: "mainPage")
}

def mainPage() {
    if(app.getInstallationState() != "COMPLETE") {state.hide=false} else {state.hide=true}

	return dynamicPage(name: "mainPage", uninstall:true, install: true) {
		section(getFormat("header","Name"),hideable: true, hidden: state.hide) {
            label title: getFormat("importantBold","Enter a name for this child app.")+
            getFormat("lessImportant","<br>This will create a virtual smoke sensor which reports the detected/clear status based on the sensors you select."), required:true,width:4
            paragraph ""
			input "enforceName", "bool", title: getFormat("importantBold","Sync child device name?")+
            getFormat("lessImportant","<br>Enable: the device and app names will sync.<br>Disabled: the device name can be changed."), defaultValue: true, displayDuringSetup: true, required: false, width: 4
		}

		section(getFormat("header","<b>Device Selection</b>")) {
			paragraph getFormat("importantBold","Please choose which sensors to include in this group.")

			input "smokeSensors", "capability.smokeDetector", title: getFormat("lessImportant","Devices to monitor"), multiple:true, required:true,width:4
        }

		section(getFormat("header","<b>Options</b>"),hideable: true, hidden: state.hide) {            
            input "activeThreshold", "number", title: getFormat("importantBold","How many sensors must detect smoke before the group is detected?")+
				getFormat("lessImportant","<br>Leave set to one if smoke detected by any sensor should change the group to detected."), required:false, defaultValue: 1,width:4
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
    subscribe(smokeSensors, "smoke", smokeHandler)
    createOrUpdateChildDevice()
    smokeHandler()
    def device = getChildDevice(state.smokeDevice)
    device.sendEvent(name: "TotalCount", value: smokeSensors.size())
    device.sendEvent(name: "SmokeDetectedThreshold", value: activeThreshold)
	if (debugOutput) {
		runIn(1800,logsOff)
	}
}

def smokeHandler(evt) {
    logInfo "Smoke status changed, checking status count..."
    getCurrentCount()
    def device = getChildDevice(state.smokeDevice)
    if (state.totalDetected >= activeThreshold)
    {
        logInfo "Detected threshold met; setting ${device.displayName} as detected"
        logDebug "Current threshold value is ${activeThreshold}"
        device.sendEvent(name: "smoke", value: "detected", descriptionText: "The detected devices are ${state.smokeDetectedList}")
    }
    else
    {
        logInfo "Detected threshold not met; setting ${device.displayName} as clear"
        logDebug "Current threshold value is ${activeThreshold}"
        device.sendEvent(name: "smoke", value: "clear")
    }
}

def createOrUpdateChildDevice() {
    def childDevice = getChildDevice("smokegroup:" + app.getId())
    state.smokeDevice = state.smokeDevice ?: "smokegroup:" + app.getId()
    if (!childDevice || state.smokeDevice == null) {
        logDebug "Creating child device"
        addChildDevice("rle.sg+", "Sensor Groups+_OmniSensor", "smokegroup:" + app.getId(), 1234, [label: app.label, isComponent: false])
    }
    if(state.hide) {
        if(enforceName) {
            if(childDevice.displayName != app.label) {
				log.warn "Child device of: ${childDevice.displayName} does not equal app name of: ${app.label}. Resetting device name."
                childDevice.label = app.label
            }
        }
    }
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

def getCurrentCount() {
    def device = getChildDevice(state.smokeDevice)
    def totalDetected = 0
    def totalClear = 0
    def smokeDetectedList = []
    smokeSensors.each { it ->
        if (it.currentValue("smoke") == "detected")
        {
            totalDetected++
            smokeDetectedList.add(it.displayName)
        }
        else if (it.currentValue("smoke") == "clear")
        {
            totalClear++
        }
    }
    state.totalDetected = totalDetected
    if (smokeDetectedList.size() == 0) {
        smokeDetectedList.add("None")
    }
    smokeDetectedList = smokeDetectedList.sort()
    state.smokeDetectedList = smokeDetectedList
    logDebug "There are ${totalDetected} sensors detecting smoke"
    logDebug "There are ${totalClear} sensors that are clear"
    device.sendEvent(name: "TotalDetected", value: totalDetected)
    device.sendEvent(name: "TotalClear", value: totalClear)
    device.sendEvent(name: "SmokeDetectedList", value: state.smokeDetectedList)

    //Create display list
    String displayList = "<ul style='list-style-type: none; margin: 0;padding: 0'>"
	smokeDetectedList.each {it ->
	displayList += "<li>${it}</li>"
	}
	displayList += "</ul>"
	device.sendEvent(name: "displayList", value: displayList)
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