import {
  AiChatLogV1alpha1Api,
  AiChatLogV1alpha1ConsoleApi,
  TestChatV1alpha1ConsoleApi,
} from "@/api/generated";
import { axiosInstance } from "@halo-dev/api-client";

const aiModelHubApiClient = {
  chatLog: new AiChatLogV1alpha1Api(undefined, "", axiosInstance),
  chatLogConsole: new AiChatLogV1alpha1ConsoleApi(undefined, "", axiosInstance),
  testChat: new TestChatV1alpha1ConsoleApi(undefined, "", axiosInstance),
};

export { aiModelHubApiClient };
