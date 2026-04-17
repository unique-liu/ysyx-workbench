#include <shoot.h>
#include <exec.h>
#include <init.h>
#include <sys/types.h>
#include <unistd.h>
#include <signal.h>
#include <sys/wait.h>

int i_am_child = 0;
int shoot_on = 0;//0: i an child process, 1: i am parent process and can shoot
pid_t shoot_pid = 0;
#ifdef CONFIG_AUTOTRACE
void shoot_clear(){
    if (shoot_on) {
        if (shoot_pid!=0) {
            kill(shoot_pid, SIGKILL);
            waitpid(shoot_pid, NULL, 0);
            DEBUG_PRINT(shoot, T, "shoot pid %d cleared\n", shoot_pid);
            shoot_pid = 0;
        }
    }
}


void shoot(){
    if (npc_state.trace_on != TRACE_AUTO) {
        return;
    }
    shoot_clear();

    shoot_pid = fork();
    if (shoot_pid == 0) {
        raise(SIGSTOP);
        DEBUG_END();
        DEBUG_APPEND();
        DEBUG_PRINT(shoot, T, "shoot pid %d weakup\n", getpid());
        npc_state.trace_on = TRACE_ON;
        shoot_on = 0;
        i_am_child = 1;
        init_fst();
    }else {
        DEBUG_PRINT(shoot, T, "create shoot pid %d\n", shoot_pid);
    }
    
}

void shoot_init(){
    if (npc_state.trace_on != TRACE_AUTO) {
        return;
    }
    shoot_on = 1;
    shoot();
}

void shoot_weakup(){
    if (shoot_on) {
        if (shoot_pid!=0) {
            DEBUG_PRINT(shoot, T, "try to weakup shoot pid %d \n----------\n", shoot_pid);
            DEBUG_END();
            kill(shoot_pid, SIGCONT);
            waitpid(shoot_pid, NULL, 0);
            DEBUG_APPEND();
            DEBUG_PRINT(shoot, T, "shoot pid %d ended\n----------\n", shoot_pid);
        }
    }
}
#else
void shoot_clear(){}
void shoot(){}
void shoot_init(){}
void shoot_weakup(){}
#endif