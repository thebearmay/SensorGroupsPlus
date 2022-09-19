/**
 *
 * SensorGroups+
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
 * v1.1		RLE		Added water, smoke, and CO detectors
 */
 
definition(
    name: "Sensor Groups+",
    namespace: "rle.sg+",
    author: "Ryan Elliott",
    description: "Creates a virtual device to track a group of sensors.",
    category: "Convenience",
	iconUrl: "",
    iconX2Url: "")

preferences {
     page(name: "mainPage", title: "", install: true, uninstall: true)
} 

def installed() {
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
	// Do nothing for now
}

def mainPage() {
    dynamicPage(name: "mainPage") {
    	isInstalled()
		if(state.appInstalled == 'COMPLETE'){
			section("${app.label}") {
				paragraph "Provides options for combining multiple sensors into a single device to provide combined updates."
			}
			section("Child Apps") {
				app(name: "contactApp+", appName: "Sensor Groups+_Contact", namespace: "rle.sg+", title: "Add a new Contact Sensor Group+ Instance", multiple: true)
				app(name: "motionApp+", appName: "Sensor Groups+_Motion", namespace: "rle.sg+", title: "Add a new Motion Sensor Group+ Instance", multiple: true)
				app(name: "waterApp+", appName: "Sensor Groups+_Water", namespace: "rle.sg+", title: "Add a new Water Sensor Group+ Instance", multiple: true)
				app(name: "smokeApp+", appName: "Sensor Groups+_Smoke", namespace: "rle.sg+", title: "Add a new Smoke Sensor Group+ Instance", multiple: true)
				app(name: "coApp+", appName: "Sensor Groups+_CO", namespace: "rle.sg+", title: "Add a new CO Sensor Group+ Instance", multiple: true)
			}
			section("General") {
       			label title: "Enter a name for this parent app (optional)", required: false
 			}
		}
	}
}

def isInstalled() {
	state.appInstalled = app.getInstallationState() 
	if (state.appInstalled != 'COMPLETE') {
		section
		{
			paragraph "Please click <b>Done</b> to install the parent app."
		}
  	}
}
