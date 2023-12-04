Thread.abort_on_exception = true

class Env; class << self
  def prominfo_query_endpoint
    ENV.fetch('PROMINFO_QUERY_ENDPOINT').freeze
  end

  def prominfo_geometry
    '{"xmin":14.0321,"ymin":45.7881,"xmax":14.8499,"ymax":46.218,"spatialReference":{"wkid":4326}}'.freeze
  end

  def prominfo_params
    {
     f: 'json',
     returnGeometry: true,
     outFields: '*',
     outSr: 4326,
     geometry: Env.prominfo_geometry
    }.freeze
  end

  def positionstack_forward_endpoint
    'http://api.positionstack.com/v1/forward'.freeze
  end

  def positionstack_api_key
    ENV.fetch('POSITIONSTACK_API_KEY').freeze
  end

  def openroute_api_key
    ENV.fetch('OPENROUTE_API_KEY').freeze
  end

  def openroute_directions_endpoint
    'https://api.openrouteservice.org/v2/directions'.freeze
  end
end; end
