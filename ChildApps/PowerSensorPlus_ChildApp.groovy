/**
 *
 * Sensor Groups+_Power
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
 * v1.1     RLE     Cleaned up logging; added device listing to child device
 */
 
definition(
    name: "Sensor Groups+_Power",
    namespace: "rle.sg+",
    author: "Ryan Elliott",
    description: "Creates a virtual device to track a group of power sensors.",
    category: "Convenience",
    parent: "rle.sg+:Sensor Groups+",
    iconUrl: "",
    iconX2Url: "")

preferences {
    page(name: "prefPowerGroup")
    page(name: "prefSettings")
}

def prefPowerGroup() {
	return dynamicPage(name: "prefPowerGroup", title: "Create a Power Group", nextPage: "prefSettings", uninstall:true, install: false) {
		section {
            label title: "<b>***Enter a name for this child app.***</b>"+
            "<br>This will create a virtual power sensor which reports the total power based on the sensors you select.", required:true
		}
	}
}

def prefSettings() {
	return dynamicPage(name: "prefSettings", title: "", install: true, uninstall: true) {
		section {
			paragraph "<b>Please choose which sensors to include in this group.</b>"

			input "powerSensors", "capability.powerMeter", title: "Power sensors to monitor", multiple:true, required:true
            
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
	subscribe(powerSensors, "power", powerHandler)
    createOrUpdateChildDevice()
    powerHandler()	
	if (debugOutput) {
		runIn(1800,logsOff)
	}
}

def powerHandler(evt) {
    log.info "Power sensor change; checking totals..."
    def totalCount = powerSensors.size()
    def device = getChildDevice(state.powerDevice)
    device.sendEvent(name: "TotalCount", value: totalCount)
    def totalPower = 0
    def newPower = 0
    def powerDevices = []
    powerSensors.each {it ->
        if (it.label) {
                newName = it.label
        }
        else if (!it.label) {
                newName = it.name
        } 
        if (it.currentPower) {
            totalPower = (totalPower + it.currentPower)
            newPower = it.currentPower
        }
        if (!it.currentPower) {
            newPower = 0
            logDebug "${newName} does not have a value for power"
        }
        logDebug "${newName} current power is ${newPower}"
        powerDevices.add(newName+":"+newPower)
    }
    powerDevices.sort()
    groovy.json.JsonOutput.toJson(powerDevices)
    log.info "Total power is ${totalPower}"
    device.sendEvent(name: "PowerDevices", value: powerDevices, unit: "W")
    device.sendEvent(name: "TotalPower", value: totalPower, unit: "W")
}


def createOrUpdateChildDevice() {
    def childDevice = getChildDevice("powergroup:" + app.getId())
    if (!childDevice || state.powerDevice == null) {
        logDebug "Creating child device"
        state.powerDevice = "powergroup:" + app.getId()
        addChildDevice("rle.sg+", "Sensor Groups+_OmniSensor", "powergroup:" + app.getId(), 1234, [name: app.label + "_device", isComponent: false])
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