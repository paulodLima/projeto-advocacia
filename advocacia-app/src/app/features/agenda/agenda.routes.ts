import { Routes } from '@angular/router';

export const agendaRoutes: Routes = [{path:'',title:'Agenda — Gestão Advocacia',data:{modulo:'agenda'},loadComponent:()=>import('./pages/agenda').then(m=>m.Agenda)}];
