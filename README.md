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
Weather data are returned in a specific format:

```json
{
  "location": "Anywhere, UK",
  "weather": {
    "id": 522,
    "description": "crappy weather"
  },
  "temperature": {
    "current": "12.4",
    "minimum": "3.7",
    "maximum": "12.6"
  },
  "wind": {
    "speed": "32.0",
    "fromDegrees": 30
  }
}
```

## Future Work
- Streaming weather data for an editable selection of cities.
- Ability to provide a numeric identifier for a location, as defined by OpenWeatherMap.
- A simple front-end project for displaying streaming weather data.

