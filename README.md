# Weather Service

A simple project that retrieves weather data from [Open Weather Map](https://openweathermap.org/).

This is intended as a very simple showcase of
- Lagom microservices, with a core service, external service adapter and persistence
- Akka streams, with back-pressure, throttling and flow recovery techniques

## Pre-requisites

- Get the source code by Git cloning this project.
- Install SBT build tool; you can
  [download latest SBT from here](https://www.scala-sbt.org/download.html).

## Run the services (development)

- From a command prompt (Git Bash recommended on Windows OS), load the SBT shell by typing "sbt".
  The first time you run this, it will take some time as it needs to download a tonne of libraries,
  but eventually you should see an sbt prompt.

  ```
  $ sbt
  .....
  [info] Loading settings from plugins.sbt ...
  [info] Loading project definition from C:\dev\lagom\weather-service\project
  [info] Loading settings from build.sbt ...
  [info] Set current project to weather-service (in build file:/C:/dev/lagom/weather-service/)
  [info] sbt server started at local:sbt-server-abcdabcdabcdabcdabcd
  sbt:weather-service>
  ```

- Compile the project, ignoring any warnings about version conflicts (which are due to Lagom 1.4
  upgrading to Akka 2.5 and Play 2.6, while maintaining backward compatibility with some of the
  libraries used in previous versions):

  ```
  sbt:weather-service> compile
  [info] Updating ...
  [info] Updating owm-adapter-api...
  [info] Done updating.
  [info] Done updating.
  [warn] Found version conflict(s) in library dependencies; some are suspected to be binary incompatible:
  ... (snip) ...
  [success] Total time: 28 s, completed 30-Jul-2018 10:45:58
  sbt:weather-service>
  ```

- Run the services using the Lagom development environment. This provides an embedded Cassandra
  instance, a service locator and gateway, and also triggers hot-reloads following code changes:

  ```
  sbt:weather-service> runAll
  [info] Starting Cassandra
  ... (snip) ...
  [info] Service owm-adapter-impl listening for HTTP on 0:0:0:0:0:0:0:0:53466
  [info] Service weather-service-impl listening for HTTP on 0:0:0:0:0:0:0:0:57487
  [info] (Services started, press enter to stop and go back to the console...)
  ```

- Check the services are running by navigating to
  - [http://localhost:9000/api/weather-service/is-alive](http://localhost:9000/api/weather-service/is-alive)
  - [http://localhost:9000/api/owm-adapter/is-alive](http://localhost:9000/api/owm-adapter/is-alive)

## Run the tests

Single run:

```
$ sbt test
```

Triggered by code changes:

```
$ sbt
.....
sbt:weather-service> ~ test
```

## API

### Weather Data

In order to retrieve weather data from OpenWeatherMap, an API key is required. This involves
creating an account (the free tier allows up to 60 requests per minute) and then
[generating an API Key](https://home.openweathermap.org/api_keys) through the UI.

You will then need to set this key as environment variable `source.owm.apiKey`. For example, when
running SBT / Lagom locally in development, you can pass it to the sbt command like this:

```sh
sbt -Dsource.owm.apiKey=YourApiKeyGoesHere runAll
```

#### Requests

- `GET http: /api/weather-service/current/[location]`

  Retrieves the current weather for a given location.
  
  It is strongly recommended to provide a two-letter country code in your query. For example, a
  search for "Wellington" will return weather for Wellington, New Zealand, but maybe you meant
  Wellington in India, South Africa, or Australia (there are two of those)?
  
  Additionally, for unambiguous results it is recommended to be as specific as possible when
  querying a location. For example, a search for "Newcastle, GB" will return weather for Newcastle
  in Monmouthshire, Wales, rather than for Newcastle upon Tyne or Newcastle-under-Lyme.

- `GET http: /api/weather-service/forecast/[location]`

  Retrieves a five-day weather forecast for a given location, at three-hourly intervals.
  
  It is strongly recommended to provide a two-letter country code in your query. For example, a
  search for "Wellington" will return weather for Wellington, New Zealand, but maybe you meant
  Wellington in India, South Africa, or Australia (there are two of those)?
  
  Additionally, for unambiguous results it is recommended to be as specific as possible when
  querying a location. For example, a search for "Newcastle, GB" will return weather for Newcastle
  in Monmouthshire, Wales, rather than for Newcastle upon Tyne or Newcastle-under-Lyme.

- `GET ws: /api/weather-service/streaming/current`

  Retrieves a continuous stream of current weather data for a pre-defined list of locations; data
  for each location in the list are emitted in turn, by default one every 3 seconds.
  
  Note that changing the update frequency or adding / removing locations will be reflected in
  currently open streams, not just newly connected ones; see
  [Streaming Parameters](#streaming-parameters).

- `GET ws: /api/weather-service/streaming/forecast`

  Retrieves a continuous stream of forecast weather data for a pre-defined list of locations; data
  for each location in the list are emitted in turn, by default one every 3 seconds. Forecast data
  are for five days, at three-hourly intervals.
  
  Note that changing the update frequency or adding / removing locations will be reflected in
  currently open streams, not just newly connected ones; see
  [Streaming Parameters](#streaming-parameters).

#### Responses
  Weather data are returned in JSON format; see example below.
  
  Units are metric: temperature in degrees celsius, wind speed in m/s, visibility in metres,
  precipitation in millimetres, humidity in percent. Note that for _current_ weather data, min and
  max temperatures are observations over the entire region at the time of measurement; these are
  likely to be the same for smaller locations, but could be different for large cities and
  metropolitan areas. For forecasts, minimum and maximum temperatures represent variation within the
  forecast period.

##### Current Weather

```json
{
  "location": "Edinburgh, GB",
  "current": {
    "measured": "2018-06-19T14:00:00+01:00",
    "weather": {
      "id": 522,
      "description": "crappy pissy rain"
    },
    "temperature": {
      "current": "12.4",
      "minimum": "10.9",
      "maximum": "12.9",
      "humidity": 96
    },
    "wind": {
      "speed": "32.0",
      "fromDegrees": 30
    },
    "sun": {
      "sunrise": "2018-06-19T04:26:06+01:00",
      "sunset": "2018-06-19T22:02:31+01:00"
    }
  }
}
```

##### Weather Forecast

```json
{
  "location": "Edinburgh, GB",
  "current": {
    "measured": "2018-06-19T14:00:00+01:00",
    "weather": {
      "id": 522,
      "description": "crappy pissy rain"
    },
    "temperature": {
      "current": "12.4",
      "minimum": "10.9",
      "maximum": "12.9",
      "humidity": 96
    },
    "wind": {
      "speed": "32.0",
      "fromDegrees": 30
    },
    "sun": {
      "sunrise": "2018-06-19T04:26:06+01:00",
      "sunset": "2018-06-19T22:02:31+01:00"
    }
  },
  "forecast": [
    {
      "measured": "2018-06-19T14:00:00+01:00",
      "weather": {
        "id": 522,
        "description": "crappy pissy rain"
      },
      "temperature": {
        "current": "12.4",
        "minimum": "10.9",
        "maximum": "12.9",
        "humidity": 96
      },
      "wind": {
        "speed": "32.0",
        "fromDegrees": 30
      }
    },
    ...
  ]
}
```

### Streaming Parameters

- `GET http: /api/weather-service/streaming/parameters`
  
  Retrieves the current parameters for streaming weather data.

  #### Response
  
  ```json
  {
    "emitFrequencySeconds": 3,
    "locations": [
      "Somewhere, GB",
      "Anywhere, FR",
      "Nowhere, US"
    ]
  }
  ```

- `PUT http: /api/weather-service/streaming/parameters/emit-frequency`
  
  Changes the emit frequency (in seconds) of the weather data stream.
  
  #### Request
  
  ```json
  {
    "frequency": 30
  }
  ```

- `POST http: /api/weather-service/streaming/parameters/locations`
  
  Adds a location to the list for the weather data stream.
  
  #### Request
  
  ```json
  {
    "location": "Somewhere Else, CA"
  }
  ```

- `DELETE http: /api/weather-service/streaming/parameters/locations/[location]`
  
  Removes the given location from the list for the weather data stream.
  
  Note that the response will still be `200 OK` even if the location was not found in the
  current list of locations.

## Future Work
- Ability to provide a numeric identifier for a location, as defined by OpenWeatherMap, for
  unambiguous identification.
- Diagnostic Context, passed in messages (or header of non-message requests) to demonstrate tracing.
- A simple front-end app for displaying streaming weather data.
- Authentication: basic auth for now? Could then demonstrate auth-check via ServiceCall composition.
