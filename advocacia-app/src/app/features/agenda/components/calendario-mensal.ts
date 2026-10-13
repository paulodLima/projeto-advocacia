import { Component, input, output } from '@angular/core';
export interface DiaCalendario {data:string;numero:number;mesAtual:boolean;total:number;pendentes:number;prazosFatais:number;}
@Component({selector:'app-calendario-mensal',templateUrl:'./calendario-mensal.html',styleUrl:'./calendario-mensal.scss'})
export class CalendarioMensal {
  readonly dias=input.required<DiaCalendario[]>();
  readonly titulo=input.required<string>();
  readonly selecionado=input('');
  readonly hoje=input('');
  readonly navegar=output<number>();
  readonly escolher=output<string>();
  readonly irHoje=output<void>();
}
