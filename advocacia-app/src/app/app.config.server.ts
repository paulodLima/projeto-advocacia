import { mergeApplicationConfig, ApplicationConfig } from '@angular/core';
import { provideServerRendering, withRoutes } from '@angular/ssr';
import { appConfig } from './app.config';
import { serverRoutes } from './app.routes.server';
import { API_URL } from './core/config/api-url.token';
import { environment } from '../environments/environment';

const serverConfig: ApplicationConfig = {
  providers: [
    {
      provide: API_URL,
      useFactory: () => process.env['API_URL'] || environment.apiUrl,
    },
    provideServerRendering(withRoutes(serverRoutes))
  ]
};

export const config = mergeApplicationConfig(appConfig, serverConfig);
