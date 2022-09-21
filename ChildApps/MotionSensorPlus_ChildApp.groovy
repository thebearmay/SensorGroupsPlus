/**
 *
 * Sensor Groups+_Motion
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
    name: "Sensor Groups+_Motion",
    namespace: "rle.sg+",
    author: "Ryan Elliott",
    description: "Creates a virtual device to track a group of motion sensors.",
    category: "Convenience",
    parent: "rle.sg+:Sensor Groups+",
    iconUrl: "",
    iconX2Url: "")

preferences {
    page(name: "prefMotionGroup")
    page(name: "prefSettings")
}

def prefMotionGroup() {
	return dynamicPage(name: "prefMotionGroup", title: "Create a Motion Group", nextPage: "prefSettings", uninstall:true, install: false) {
		section {
            label title: "Enter a name for this child app. This will create a virtual motion sensor which reports the active/inactive status based on the sensors you select.", required:true
		}
	}
}

def prefSettings() {
	return dynamicPage(name: "prefSettings", title: "", install: true, uninstall: true) {
		section {
			paragraph "Please choose which sensors to include in this group. When the count of active sensors is at or above the threshold, the virtual sensor will be active. When the count is below the threshold, the virtual sensor will be inactive."

			input "motionSensors", "capability.motionSensor", title: "Motion sensors to monitor", multiple:true, required:true
        }
        section {
            paragraph "Set how many sensors are required to change the status of the virtual device."
            
            input "activeThreshold", "number", title: "How many sensors must be active before the group is active? Leave set to one if any motion sensor active should make the group active.", required:false, defaultValue: 1
            
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
	subscribe(motionSensors, "motion", motionHandler)
    createOrUpdateChildDevice()
    motionHandler()
    def device = getChildDevice(state.motionDevice)
	device.sendEvent(name: "TotalCount", value: motionSensors.size())
    device.sendEvent(name: "ActiveThreshold", value: activeThreshold) 
	if (debugOutput) {
		runIn(1800,logsOff)
	}
}

def motionHandler(evt) {
    log.info "Motion sensor change; checking totals..."
    def device = getChildDevice(state.motionDevice)
    getCurrentCount()
    if (state.totalActive >= activeThreshold) {
        log.info "Active threshold met; setting group device as active"
        logDebug "Current threshold value is ${activeThreshold}"
        device.sendEvent(name: "motion", value: "active", descriptionText: "The detected devices are ${state.activeList}")
    } else {
        log.info "Active threshold not met; setting group device as inactive"
        logDebug "Current threshold value is ${activeThreshold}"
        device.sendEvent(name: "motion", value: "inactive")
    }
}


def createOrUpdateChildDevice() {
    def childDevice = getChildDevice("motiongroup:" + app.getId())
    if (!childDevice || state.motionDevice == null) {
        logDebug "Creating child device"
        state.motionDevice = "motiongroup:" + app.getId()
        addChildDevice("rle.sg+", "Sensor Groups+_Virtual Motion Sensor", "motiongroup:" + app.getId(), 1234, [name: app.label + "_device", isComponent: false])
    } else if (childDevice && childDevice.name != app.label)
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
    def device = getChildDevice(state.motionDevice)
    def totalActive = 0
    def totalInactive = 0
    def activeList = []
    motionSensors.each { it ->
        if (it.currentValue("motion") == "active") 
        {
            totalActive++
			if (it.label) {
                activeList.add(it.label)
            }
            else if (!it.label) {
                activeList.add(it.name)
            }
        } 
        else if (it.currentValue("motion") == "inactive") 
        {
			totalInactive++
		}
    }
    state.totalActive = totalActive
    if (activeList.size() == 0) {
        activeList.add("None")
    }
    state.activeList = activeList.sort()
    logDebug "There are ${totalActive} sensors active."
    logDebug "There are ${totalInactive} sensors inactive."
    device.sendEvent(name: "TotalActive", value: totalActive)
    device.sendEvent(name: "TotalInactive", value: totalInactive)
    device.sendEvent(name: "ActiveList", value: state.activeList)
}