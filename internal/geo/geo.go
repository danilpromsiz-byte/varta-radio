package geo

import "math"

const earthRadiusKm = 6371.0

// Distance calculates the Haversine distance in kilometers between two lat/lon coordinates.
func Distance(lat1, lon1, lat2, lon2 float64) float64 {
	dLat := degreesToRadians(lat2 - lat1)
	dLon := degreesToRadians(lon2 - lon1)

	rLat1 := degreesToRadians(lat1)
	rLat2 := degreesToRadians(lat2)

	a := math.Sin(dLat/2)*math.Sin(dLat/2) +
		math.Cos(rLat1)*math.Cos(rLat2)*math.Sin(dLon/2)*math.Sin(dLon/2)

	c := 2 * math.Atan2(math.Sqrt(a), math.Sqrt(1-a))

	return earthRadiusKm * c
}

// WithinRadius checks if two points are within the given radius in kilometers.
func WithinRadius(lat1, lon1, lat2, lon2, radiusKm float64) bool {
	return Distance(lat1, lon1, lat2, lon2) <= radiusKm
}

func degreesToRadians(deg float64) float64 {
	return deg * math.Pi / 180.0
}
