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
    page(name: "prefSmokeGroup")
    page(name: "prefSettings")
}

def prefSmokeGroup() {
    return dynamicPage(name: "prefSmokeGroup", title: "Create a Smoke Sensor Group", nextPage: "prefSettings", uninstall:true, install: false) {
        section {
            label title: "Enter a name for this child app. This will create a virtual smoke sensor which reports the detected/clear status based on the sensors you select.", required:true
        }
    }
}

def prefSettings() {
    return dynamicPage(name: "prefSettings", title: "", install: true, uninstall: true) {
        section {
            paragraph "Please choose which sensors to include in this group. When all the sensors are clear, the virtual device is clear. If any sensor has detected smoke, the virtual device is detected."

            input "smokeSensors", "capability.smokeDetector", title: "Smoke sensors to monitor", multiple:true, required:true
        }
        section {
            paragraph "Set how many sensors are required to change the status of the virtual device."
            
            input "activeThreshold", "number", title: "How many sensors must detect smoke before the group is detected? Leave set to one if smoke detected by any sensor should change the group to detected.", required:false, defaultValue: 1
            
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
    runIn(1800,logsOff)
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
        addChildDevice("rle.sg+", "Sensor Groups+_Virtual Smoke Detector", "smokegroup:" + app.getId(), 1234, [name: app.label + "_device", isComponent: false])
    }
    else if (childDevice && childDevice.name != (app.label + "_device"))
        childDevice.name = app.label + "_device"
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
			if (it.label) {
                smokeDetectedList.add(it.label)
            }
            else if (!it.label) {
                smokeDetectedList.add(it.name)
            }
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
    state.smokeDetectedList = smokeDetectedList.sort()
    logDebug "There are ${totalDetected} sensors detecting smoke"
    logDebug "There are ${totalClear} sensors that are clear"
    device.sendEvent(name: "TotalDetected", value: totalDetected)
    device.sendEvent(name: "TotalClear", value: totalClear)
    device.sendEvent(name: "SmokeDetectedList", value: state.smokeDetectedList)
}