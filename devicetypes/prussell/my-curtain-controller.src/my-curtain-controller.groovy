/**
 *  My Aeon Motor Controller
 *
 *  Copyright 2017 Philip Russell
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	definition (name: "My Curtain Controller", namespace: "prussell", author: "Philip Russell") {
		capability "Refresh"
		capability "Actuator"
        		capability "doorControl"
        		capability "Switch"
command "Close"
command "Open"
command "stop"
//fingerprint inClusters: "0x26,0x32"
	}
simulator {
status "Close":   "command: 2604, payload: FF"
status "Open": "command: 2604, payload: 00"
status "stop": "command: 2605, payload: FE"
["FF", "FE", "00"].each { val ->
reply "2001$val,delay 100,2602": "command: 2603, payload: $val"
		}
	}	
tiles {
	standardTile("motor", "device.motor", width: 2, height: 2) {
		state("stopClose", label:'StopCls', icon:"st.doors.garage.garage-closed", action: 'Open', backgroundColor:"#79b821")
		state("stopOpn", label:'StopOpn', icon:"st.doors.garage.garage-open", action: 'Close', backgroundColor:"#79b821")
		state("Close", label:'Close', icon:"st.doors.garage.garage-closing", action:'stop', backgroundColor:"#ffe71e")
		state("Open", label:'Open', icon:"st.doors.garage.garage-opening", action:'stop', backgroundColor:"#ffe71e")
		}
	valueTile("energy", "device.energy", decoration: "flat") {
		state "default", label:' '
		}
	standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
		state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
	standardTile("stop", "device.switch") {
        		state "default", label:"", action: "stop", icon:"http://cdn.device-icons.smartthings.com/sonos/stop-btn@2x.png"
        		}
	standardTile("Close", "device.switch") {
        		state "default", label: "Cls", action: "Close", icon:"http://cdn.device-icons.smartthings.com/thermostat/thermostat-down@2x.png"
		}
	standardTile("Open", "device.switch") {
        		state "default", label: "Opn", action: "Open", icon:"http://cdn.device-icons.smartthings.com/thermostat/thermostat-up@2x.png"
		}
	}
main(["motor"])
	details(["motor",  "refresh", "energy", "Close", "Open", "stop",])
	}

// parse events into attributes
	def parse(String description) {
		def result = []
		def cmd = zwave.parse(description, [0x20: 1, 0x26: 3])
		if (cmd) {
			result = zwaveEvent(cmd)
			log.debug("'$description' parsed to $result")
		} else {
			log.debug("Couldn't zwave.parse '$description'")
			}
		result
		}

		def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
			motorEvents(cmd)
			}

		def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
			motorEvents(cmd)
			}

		def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd) {
			motorEvents(cmd)
			}

		def motorEvents(physicalgraph.zwave.Command cmd) {
		def result = []
    		def switchEvent = []
		if(cmd.value == 0) {switchEvent = createEvent(name: "motor", value: "Open", descriptionText: text)}
			else if(cmd.value == 254) {
    				def stopVal = state.Close ? "stopClose" : "stopOpn"
		    	switchEvent = createEvent(name: "motor", value: stopVal, descriptionText: text)
    			}
    		else if(cmd.value == 255) {switchEvent = createEvent(name: "motor", value: "Close", descriptionText: text)}
			result << switchEvent
		}

		def refresh() {
			zwave.switchMultilevelV1.switchMultilevelGet().format()
		}

		def Close() {
			state.Close = true
			delayBetween([
    				zwave.basicV1.basicSet(value: 0xFF).format(),
				zwave.switchMultilevelV1.switchMultilevelGet().format()
    			], 1000)
		}

		def Open() {
			state.Close = false
			delayBetween([
    				zwave.basicV1.basicSet(value: 0x00).format(),
				zwave.switchMultilevelV1.switchMultilevelGet().format()
			], 1000)
		}

		def stop() {
			delayBetween([
    				zwave.switchMultilevelV1.switchMultilevelStopLevelChange().format(),
				zwave.switchMultilevelV1.switchMultilevelGet().format()
			], 1000)
		}

		def on() {
			state.Close = true
			delayBetween([
				zwave.basicV1.basicSet(value: 0xFF).format(),
				zwave.switchMultilevelV1.switchMultilevelGet().format()
			], 1000)
		}

		def off() {
			state.Close = false
			delayBetween([
    				zwave.basicV1.basicSet(value: 0x00).format(),
				zwave.switchMultilevelV1.switchMultilevelGet().format()
			], 1000)
		}