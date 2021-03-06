/**
 *  MQTT SHM Bridge
 *
 *  Authors
 *   - open-source@ethitter.com
 *
 *  Copyright 2016
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.transform.Field

definition(
    name: "MQTT SHM Bridge",
    namespace: "eth",
    author: "erick t. hitter",
    description: "Control Smart Home Monitor over MQTT",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Connections/Cat-Connections.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Connections/Cat-Connections@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Connections/Cat-Connections@3x.png"
)

preferences {
    section ("Bridge") {
        input "bridge", "capability.notification", title: "Notify this Bridge", required: true, multiple: false
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    runEvery15Minutes(initialize)
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    // Unsubscribe from all events
    unsubscribe()
    // Subscribe to stuff
    initialize()
}

// Return list of displayNames
def getDeviceNames(devices) {
    def list = []
    devices.each{device->
        list.push(device.displayName)
    }
    list
}

def initialize() {
    // Subscribe to events from SmartThings
    subscribe(location, "alarmSystemStatus", inputHandler)
    subscribe(location, "Security", inputHandler)

    // Subscribe to events from the bridge
    subscribe(bridge, "message", bridgeHandler)

    // Update the bridge
    updateSubscription()
}

// Update the bridge"s subscription
def updateSubscription() {
    def attributes = [
        alarmSystemStatus: ["alarm system status"]
    ]

    def json = new groovy.json.JsonOutput().toJson([
        path: "/subscribe",
        body: [
            devices: attributes
        ]
    ])

    log.debug "Updating subscription: ${json}"

    bridge.deviceNotification(json)
}

// Receive an event from the bridge
def bridgeHandler(evt) {
    def json = new JsonSlurper().parseText(evt.value)
    log.debug "Received device event from bridge: ${json}"

    if (json.type == "alarmSystemStatus") {
        sendLocationEvent(name: "alarmSystemStatus", value: "${json.value}")
        return
    }
}

// Receive an event from a device
def inputHandler(evt) {
    def value

    switch(evt.value) {
        case "stay":
            value = "armed_home"
        break
        case "away":
            value = "armed_away"
        break
        case "off":
            value = "disarmed"
        break

        default:
            value = false
        break
    }

    if ( value == false ) {
       log.debug "Unknown SHM event: ${evt.value}"
       return
    }

    def json = new JsonOutput().toJson([
        path: "/push",
        body: [
            name: evt.displayName,
            value: value,
            type: evt.name
        ]
    ])

    log.debug "Forwarding device event to bridge: ${json}"
    bridge.deviceNotification(json)
}