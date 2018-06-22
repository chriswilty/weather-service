# Weather Service

A simple project that retrieves weather data from [Open Weather Map](https://openweathermap.org/).

This is intended as a very simple showcase of
- Lagom microservices, with external service adapters and persistence
- Akka streams, with back-pressure, throttling and flow recovery techniques

## API

### Weather Data

#### Requests

- `GET http: /api/weather-service/current/[location]`

  Retrieves current weather data for a given location.
  
  It is strongly recommended to provide a two-letter country code in your query. For example, a
  search for "Wellington" will return weather for Wellington, New Zealand, but maybe you meant
  Wellington in India, South Africa, or Australia (there are two of those)?
  
  Additionally, for unambiguous results it is recommended to be as specific as possible when
  querying a location. For example, a search for "Newcastle, GB" will return weather for Newcastle
  in Monmouthshire, Wales, rather than for Newcastle upon Tyne or Newcastle-under-Lyme.

- `GET ws: /api/weather-service/streaming/current`

  Retrieves a continuous stream of current weather data for a pre-defined list of locations; data
  for each location in the list are emitted in turn, one every 3 seconds. In a future release, users
  will be able to add and remove locations, and modify the emit frequency.

#### Response
  Weather data are returned in a specific format; see example below.
  Units are metric, i.e. temperature in degrees celsius, wind speed in m/s, visibility in metres,
  precipitation in millimetres. Note that for _current_ weather data, min and max temperatures
  are for the entire region at the time of measurement; these are likely to be the same for smaller
  locations, but could be different for larger cities and metropolitan areas. For forecasts, minimum
  and maximum temperatures will represent the forecasted variation throughout the day.

```json
{
  "location": "Edinburgh, GB",
  "measured": "2018-06-19T14:00:00+01:00",
  "weather": {
    "id": 522,
    "description": "crappy pissy rain"
  },
  "temperature": {
    "current": "12.4",
    "minimum": "10.9",
    "maximum": "12.9"
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
- Real-time changes to open weather streams when parameters are updated
- 5-day weather forecast
- Ability to provide a numeric identifier for a location, as defined by OpenWeatherMap, for
  unambiguous identification.
- A simple front-end app for displaying streaming weather data.
