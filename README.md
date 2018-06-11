# Weather Service

A simple project that retrieves weather data from [Open Weather Map](https://openweathermap.org/).

## API

- `GET /api/weather-service/current/[location]`

  Retrieves current weather data for a given location.
  
  It is strongly recommended to provide a two-letter country code in your query. For example, a
  search for "Wellington" will return weather for Wellington, New Zealand, but maybe you meant
  Wellington in India, South Africa, or Australia (there are two of those)?
  
  Additionally, for unambiguous results it is recommended to be as specific as possible when
  querying a location. For example, a search for "Newcastle, GB" will return weather for Newcastle
  in Monmouthshire, Wales, rather than for Newcastle upon Tyne or Newcastle-under-Lyme.

## Responses
Weather data are returned in a specific format; see example below.
Units are metric, i.e. temperature in degrees celsius, wind speed in m/s, visibility in metres,
precipitation in millimetres. Note that for _current_ weather data, minimum and maximum temperatures
are for the entire region at the time of measurement; these are likely to be the same for smaller
locations, but could be different for larger cities and metropolitan areas. For forecasts, minimum
and maximum temperatures will represent the forecasted variation throughout the day.

```json
{
  "location": "Anywhere, UK",
  "weather": {
    "id": 522,
    "description": "crappy weather"
  },
  "temperature": {
    "current": "12.4",
    "minimum": "10.9",
    "maximum": "12.9"
  },
  "wind": {
    "speed": "32.0",
    "fromDegrees": 30
  }
}
```

## Future Work
- Streaming weather data for an editable selection of cities.
- 5-day weather forecast
- Ability to provide a numeric identifier for a location, as defined by OpenWeatherMap, for
  unambiguous identification.
- A simple front-end app for displaying streaming weather data.

