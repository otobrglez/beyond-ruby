from random import choice

from locust import FastHttpUser, task, between, tag


class StationsUser(FastHttpUser):
    wait_time = between(1, 5)
    queries = ["BTC Ljubljana", "Dunajska 5, Ljubljana", "Kongresni trg, Ljubljana"]

    @tag('v1', 'near')
    @task
    def v1_near(self):
        query = choice(self.queries)
        self.client.get("/v1/near?query={}".format(query), name="v1_near")

    @tag('v2', 'near')
    @task
    def v2_near(self):
        query = choice(self.queries)
        self.client.get("/v2/near?query={}".format(query), name="v2_near")

    @tag('v3', 'distance')
    @task
    def v3_near(self):
        query = choice(self.queries)
        self.client.get("/v3/near?query={}".format(query), name="v3_near")

    @tag('v4', 'distance')
    @task
    def v4_near(self):
        query = choice(self.queries)
        self.client.get("/v4/near?query={}".format(query), name="v4_near")
