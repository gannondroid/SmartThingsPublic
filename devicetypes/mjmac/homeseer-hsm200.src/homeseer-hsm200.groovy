/**
 *  HomeSeer HSM200
 *
 *  Based on data from http://www.expresscontrols.com/pdf/EZMultiPliOwnerManual.pdf
 * 
 * FYI: This driver is not completely working (yet?). See the following link for details:
 * http://community.smartthings.com/t/homeseer-hsm200-support/9209
 *
 *  Copyright 2015 Michael MacDonald
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
  definition (name: "HomeSeer HSM200", namespace: "mjmac", author: "Michael MacDonald") {
        capability "Switch"
    capability "Motion Sensor"
    capability "Configuration"
    capability "Illuminance Measurement"
    capability "Temperature Measurement"
    capability "Color Control"

    fingerprint inClusters: "0x71,0x31,0x33,0x72,0x86,0x59,0x85,0x70,0x77,0x5A,0x7A,0x73,0xEF,0x20", deviceId: "0x0701"
  }

  simulator {
    // TODO: define status and reply messages here
  }

  tiles {
        standardTile("switch", "device.switch", canChangeIcon: true) {
            state "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"turningOff"
            state "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
            state "turningOn", label:'${name}', icon:"st.switches.switch.on", backgroundColor:"#79b821"
            state "turningOff", label:'${name}', icon:"st.switches.switch.off", backgroundColor:"#ffffff"
        }
        standardTile("motion", "device.motion") {
      state "active", label:'motion', icon:"st.motion.motion.active", backgroundColor:"#53a7c0"
      state "inactive", label:'no motion', icon:"st.motion.motion.inactive", backgroundColor:"#ffffff"
    }
        controlTile("rgbSelector", "device.color", "color", height: 3, width: 3, inactiveLabel: false) {
         state "color", action:"color control.setColor"
      }
    valueTile("temperature", "device.temperature", inactiveLabel: false) {
      state "temperature", label:'${currentValue}Â°',
      backgroundColors:[
        [value: 31, color: "#153591"],
        [value: 44, color: "#1e9cbb"],
        [value: 59, color: "#90d2a7"],
        [value: 74, color: "#44b621"],
        [value: 84, color: "#f1d801"],
        [value: 95, color: "#d04e00"],
        [value: 96, color: "#bc2323"]
      ]
    }        
        valueTile("illuminance", "device.illuminance", inactiveLabel: false) {
      state "luminosity", label:'${currentValue} ${unit}', unit:"%"
    }
    standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat") {
      state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
    }
        
        main(["motion", "temperature"])
        details(["configure", "temperature", "illuminance", "motion", "switch", "rgbSelector"])
  }

  preferences {
    input "onTime", "number", title: "No motion reporting interval", description: "Send no motion report after N minutes of no motion", defaultValue: 1, displayDuringSetup: true, required: false
        input "onLevel", "number", title: "Dimmer level", description: "Dimmer setting to use when associated lights are turned on", defaultValue: 100, displayDuringSetup: true, required: false
        input "liteMin", "number", title: "Luminance reporting interval", description: "Send luminance reports every N minutes", defaultValue: 10, displayDuringSetup: true, required: false
        input "tempMin", "number", title: "Temperature reporting interval", description: "Send temperature reports every N minutes", defaultValue: 10, displayDuringSetup: true, required: false
  }
}

// parse events into attributes
def parse(String description) {
  log.debug "Parsing '${description}'"
    
    def result = null
    def cmd = zwave.parse(description)
    if (cmd) {
        result = createEvent(zwaveEvent(cmd))
        log.debug "Parsed ${cmd} to ${result.inspect()}"
    } else {
        log.debug "Non-parsed event: ${description}"
    }
    
    return result
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    // This will capture any commands not handled by other instances of zwaveEvent
    // and is recommended for development so you can see every command the device sends
    log.debug "Fallthrough cmd: ${cmd}"
    return createEvent(descriptionText: "${device.displayName}: ${cmd}")
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
    [name: "switch", value: cmd.value ? "on" : "off", type: "digital"]
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd) {
    log.debug "Sensor event: ${cmd}"
    def map = [:]
    switch (cmd.sensorType) {
        case 1:
            // temperature
            def cmdScale = cmd.scale == 1 ? "F" : "C"
            map.value = convertTemperatureIfNeeded(cmd.scaledSensorValue, cmdScale, cmd.precision)
            map.unit = getTemperatureScale()
            map.name = "temperature"
      break;
        case 3:
            // luminance
            map.value = cmd.scaledSensorValue.toInteger().toString()
            map.unit = "%"
            map.name = "illuminance"
            break;
        default:
            log.debug "Unhandled sensor event: ${cmd}"
    }
    return map
}

def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd) {
    log.debug "Notification event: ${cmd}"
    
    if (cmd.notificationType != 7) {
        log.debug "Unknown notification type: ${cmd}"
        return null
    }

    def motionState = cmd.event == 0 ? "inactive" : "active"
    return [name:"motion", value:motionState]
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {
  log.debug "Configuration Report for parameter ${cmd.parameterNumber}: Value is ${cmd.configurationValue}, Size is ${cmd.size}"
}

def on() {
  log.debug "Executing 'on'"
    delayBetween([
      zwave.basicV1.basicSet(value: 0xFF).format(),
    zwave.basicV1.basicGet().format()
    ], 1000)
}

def off() {
  log.debug "Executing 'off'"
    delayBetween([
      zwave.basicV1.basicSet(value: 0x00).format(),
    zwave.basicV1.basicGet().format()
    ], 1000)
}

def _safeSetting(value) {
    def intVal = value.toInteger()
    switch (intVal) {
      case { it < 0 }:
        return 0
        break
      case { it > 127 }:
        return 127
        break
      default:
        return intVal
    }
}

def configure() {
  log.debug "Executing 'configure'"
    def cmds = []
    cmds << zwave.configurationV2.configurationSet(parameterNumber: 1, size: 1, configurationValue: [_safeSetting(settings.onTime)]).format()
    cmds << zwave.configurationV2.configurationSet(parameterNumber: 3, size: 1, configurationValue: [_safeSetting(settings.liteMin)]).format()
    cmds << zwave.configurationV2.configurationSet(parameterNumber: 4, size: 1, configurationValue: [_safeSetting(settings.tempMin)]).format()
    
    // The device wants -1 for "on" (the default), or an integer from 0-99 
    def onLevel = settings.onLevel.toInteger()
    switch (onLevel) {
      case 100:
        onLevel = -1
        break
      case { it < 0 }:
        onLevel = 0
        break
      case { it > 99 }:
        onLevel = 99
        break
    }
    cmds << zwave.configurationV2.configurationSet(parameterNumber: 2, size: 1, configurationValue: [onLevel]).format()
    
    //for (i in 1..4) {
    //  cmds << zwave.configurationV2.configurationGet(parameterNumber: i).format()
    //}
  delayBetween(cmds, 500)
}

def setColor(value) {
    log.debug "setColor() got: ${value}"
    def cmds = []
    for (color in [[name: "red", id: 2], [name: "green", id: 3], [name: "blue", id: 4]]) {
      log.debug "Setting ${color.name} to ${value[color.name]}"
      cmds << zwave.colorControlV1.stateSet(stateDataLength: color.id).format()
      cmds << zwave.colorControlV1.startCapabilityLevelChange(capabilityId: color.id, startState: value[color.name], ignoreStartState: True, updown: True).format()
//      cmds << zwave.colorControlV1.stopStateChange(capabilityId: color.id).format()
    }
    delayBetween(cmds, 100)
}