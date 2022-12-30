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
 * v1.0		RLE		Creation
 * v1.1     RLE     Added list attribute to show triggered devices
 * v1.2     RLE     Added threshold input and associated logic
 * v1.3		RLE		UI update
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
	return dynamicPage(name: "mainPage", uninstall:true, install: true) {
		section(getFormat("header","<b>App Name</b>")) {
            label title: "<b>Enter a name for this child app.</b>"+
            "<br>This will create a virtual smoke sensor which reports the detected/clear status based on the sensors you select.", required:true,width:6
		}

		section(getFormat("header","<b>Device Selection</b>")) {
			paragraph "<b>Please choose which sensors to include in this group.</b>"+
            "<br>The virtual device will report status based on the configured threshold."

            input "smokeSensors", "capability.smokeDetector", title: "Smoke sensors to monitor", multiple:true, required:true
        }
        section(getFormat("header","<b>Options</b>")) {
            input "activeThreshold", "number", title: "<b>Threshold: How many sensors must detect smoke before the group is detected?</b><br>Leave set to one if smoke detected by any sensor should change the group to detected.", required:false, defaultValue: 1,width:6
            paragraph ""
            input "debugOutput", "bool", title: "Enable debug logging?", defaultValue: true, displayDuringSetup: false, required: false
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
    log.info "Smoke status changed, checking status count..."
    getCurrentCount()
    def device = getChildDevice(state.smokeDevice)
    if (state.totalDetected >= activeThreshold)
    {
        log.info "Detected threshold met; setting group device as detected"
        logDebug "Current threshold value is ${activeThreshold}"
        device.sendEvent(name: "smoke", value: "detected", descriptionText: "The detected devices are ${state.smokeDetectedList}")
    }
    else
    {
        log.info "Detected threshold not met; setting virtual device as clear"
        logDebug "Current threshold value is ${activeThreshold}"
        device.sendEvent(name: "smoke", value: "clear")
    }
}

def createOrUpdateChildDevice() {
    def childDevice = getChildDevice("smokegroup:" + app.getId())
    if (!childDevice || state.smokeDevice == null) {
        logDebug "Creating child device"
        state.smokeDevice = "smokegroup:" + app.getId()
        addChildDevice("rle.sg+", "Sensor Groups+_OmniSensor", "smokegroup:" + app.getId(), 1234, [name: app.label, isComponent: false])
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
    smokeDetectedList = groovy.json.JsonOutput.toJson(smokeDetectedList)
    state.smokeDetectedList = smokeDetectedList
    logDebug "There are ${totalDetected} sensors detecting smoke"
    logDebug "There are ${totalClear} sensors that are clear"
    device.sendEvent(name: "TotalDetected", value: totalDetected)
    device.sendEvent(name: "TotalClear", value: totalClear)
    device.sendEvent(name: "SmokeDetectedList", value: state.smokeDetectedList)
}

def getFormat(type, myText="") {
	if(type == "header") return "<div style='color:#660000;font-weight: bold'>${myText}</div>"
}