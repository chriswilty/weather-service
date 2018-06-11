# Weather Service

A simple project that retrieves current weather data, from [Open Weather Map](https://openweathermap.org/).

The core service provides an endpoint for retrieving current weather data for a given location. Response is in the following format:

```json
{
  location: String,
  weather: {
    id: Number,
	description: String
  },
  temperature: {
    current: Decimal (as String),
	minimum: Decimal (as String),
	maximum: Decimal (as String)
  },
  wind: {
    speed: Decimal (as String),
	fromDegrees: Number [0 - 359]
  }
}
```

For unambiguous results, it is recommended to be as specific as possible when querying a location, such as by providing a two-letter country code, for example:
- London,UK
- Paris,FR
- New York,US

A simple front-end project will also be provided for displaying streaming weather data. More info on that to follow.

