/**
 *
 * Sensor Groups+_Water
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
        name: "Sensor Groups+_Water",
        namespace: "rle.sg+",
        author: "Ryan Elliott",
        description: "Creates a virtual device to track a group of water sensors.",
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
            "<br>This will create a virtual water sensor which reports the wet/dry status based on the sensors you select.", required:true,width:6
		}

		section(getFormat("header","<b>Device Selection</b>")) {
			paragraph "<b>Please choose which sensors to include in this group.</b>"+
            "<br>The virtual device will report status based on the configured threshold."

            input "waterSensors", "capability.waterSensor", title: "Water sensors to monitor", multiple:true, required:true,width:6
        }

		section(getFormat("header","<b>Options</b>")) {
            input "activeThreshold", "number", title: "<b>How many sensors must be wet before the group is wet?</b><br>Leave set to one if any sensor being wet should make the group wet.", required:false, defaultValue: 1,width:6
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
    subscribe(waterSensors, "water", waterHandler)
    createOrUpdateChildDevice()
    waterHandler()
    def device = getChildDevice(state.waterDevice)
    device.sendEvent(name: "TotalCount", value: waterSensors.size())
    device.sendEvent(name: "WetThreshold", value: activeThreshold)
	if (debugOutput) {
		runIn(1800,logsOff)
	}
}

def waterHandler(evt) {
    log.info "Water changed, checking status count..."
    getCurrentCount()
    def device = getChildDevice(state.waterDevice)
    if (state.totalWet >= activeThreshold)
    {
        log.info "Wet threshold met; setting virtual device as wet"
        logDebug "Current threshold value is ${activeThreshold}"
        device.sendEvent(name: "water", value: "wet", descriptionText: "The wet devices are ${state.wetList}")
    }
    else
    {
        log.info "Wet threshold not met; setting virtual device as dry"
        logDebug "Current threshold value is ${activeThreshold}"
        device.sendEvent(name: "water", value: "dry")
    }
}

def createOrUpdateChildDevice() {
    def childDevice = getChildDevice("watergroup:" + app.getId())
    if (!childDevice || state.waterDevice == null) {
        logDebug "Creating child device"
        state.waterDevice = "watergroup:" + app.getId()
        addChildDevice("rle.sg+", "Sensor Groups+_OmniSensor", "watergroup:" + app.getId(), 1234, [name: app.label, isComponent: false])
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
    def device = getChildDevice(state.waterDevice)
    def totalWet = 0
    def totalDry = 0
    def wetList = []
    waterSensors.each { it ->
        if (it.currentValue("water") == "wet")
        {
            totalWet++
			wetList.add(it.displayName)
        }
        else if (it.currentValue("water") == "dry")
        {
            totalDry++
        }
    }
    state.totalWet = totalWet
    if (wetList.size() == 0) {
        wetList.add("None")
    }
    wetList = wetList.sort()
    groovy.json.JsonOutput.toJson(wetList)
    state.wetList = wetList
    logDebug "There are ${totalDry} sensors dry"
    logDebug "There are ${totalWet} sensors wet"
    device.sendEvent(name: "TotalDry", value: totalDry)
    device.sendEvent(name: "TotalWet", value: totalWet)
    device.sendEvent(name: "WetList", value: state.wetList)
}

def getFormat(type, myText="") {
	if(type == "header") return "<div style='color:#660000;font-weight: bold'>${myText}</div>"
}